package com.gilrossi.despesas.security;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OperationalRequestSignatureVerifier {

	private static final Set<String> FORBIDDEN_HOUSEHOLD_FIELDS = Set.of("householdId", "household_id");

	private final OperationalEmailIngestionProperties properties;
	private final OperationalReplayProtectionService replayProtectionService;
	private final ObjectMapper objectMapper;

	public OperationalRequestSignatureVerifier(
		OperationalEmailIngestionProperties properties,
		OperationalReplayProtectionService replayProtectionService,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.replayProtectionService = replayProtectionService;
		this.objectMapper = objectMapper;
	}

	public OperationalRequestVerificationResult verify(CachedBodyHttpServletRequest request) {
		String keyId = requiredHeader(request, OperationalRequestSignatureSupport.KEY_ID_HEADER);
		String timestampHeader = requiredHeader(request, OperationalRequestSignatureSupport.TIMESTAMP_HEADER);
		String nonce = requiredHeader(request, OperationalRequestSignatureSupport.NONCE_HEADER);
		String providedBodyHash = requiredHeader(request, OperationalRequestSignatureSupport.BODY_SHA256_HEADER).toLowerCase(Locale.ROOT);
		String providedSignature = requiredHeader(request, OperationalRequestSignatureSupport.SIGNATURE_HEADER).toLowerCase(Locale.ROOT);
		String nonceFingerprint = OperationalRequestSignatureSupport.fingerprint(nonce);
		String bodyHashPrefix = OperationalRequestSignatureSupport.prefix(providedBodyHash);

		long timestamp = parseTimestamp(timestampHeader, keyId, nonceFingerprint, bodyHashPrefix);
		Instant now = Instant.now();
		validateClockWindow(timestamp, now, keyId, nonceFingerprint, bodyHashPrefix);

		Map<String, String> secrets = properties.signingSecrets();
		String secret = secrets.get(keyId);
		if (!StringUtils.hasText(secret)) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.INVALID_KEY_ID,
				keyId,
				nonceFingerprint,
				bodyHashPrefix,
				null
			);
		}

		byte[] requestBody = request.getCachedBody();
		String computedBodyHash = OperationalRequestSignatureSupport.sha256Hex(requestBody);
		if (!OperationalRequestSignatureSupport.constantTimeEquals(computedBodyHash, providedBodyHash)) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.INVALID_BODY_HASH,
				keyId,
				nonceFingerprint,
				bodyHashPrefix,
				null
			);
		}

		String canonicalPayload = OperationalRequestSignatureSupport.canonicalPayload(
			request.getMethod(),
			request.getRequestURI(),
			keyId,
			timestampHeader,
			nonce,
			computedBodyHash
		);
		String expectedSignature = OperationalRequestSignatureSupport.hmacSha256Hex(secret, canonicalPayload);
		if (!OperationalRequestSignatureSupport.constantTimeEquals(expectedSignature, providedSignature)) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.INVALID_SIGNATURE,
				keyId,
				nonceFingerprint,
				OperationalRequestSignatureSupport.prefix(computedBodyHash),
				null
			);
		}

		JsonNode payload = parsePayload(requestBody, keyId, nonceFingerprint, OperationalRequestSignatureSupport.prefix(computedBodyHash));
		String sourceAccount = extractSourceAccount(payload);

		replayProtectionService.registerNonce(
			keyId,
			nonce,
			request.getMethod(),
			request.getRequestURI(),
			now,
			properties.getNonceTtlSeconds()
		);

		for (String forbiddenField : FORBIDDEN_HOUSEHOLD_FIELDS) {
			if (payload.hasNonNull(forbiddenField)) {
				throw new OperationalRequestValidationException(
					OperationalRequestRejectionReason.FORBIDDEN_HOUSEHOLD_FIELD,
					keyId,
					nonceFingerprint,
					OperationalRequestSignatureSupport.prefix(computedBodyHash),
					sourceAccount
				);
			}
		}

		return new OperationalRequestVerificationResult(
			keyId,
			nonceFingerprint,
			OperationalRequestSignatureSupport.prefix(computedBodyHash),
			sourceAccount
		);
	}

	private String requiredHeader(CachedBodyHttpServletRequest request, String headerName) {
		String value = request.getHeader(headerName);
		if (!StringUtils.hasText(value)) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.MISSING_REQUIRED_HEADER,
				request.getHeader(OperationalRequestSignatureSupport.KEY_ID_HEADER),
				OperationalRequestSignatureSupport.fingerprint(request.getHeader(OperationalRequestSignatureSupport.NONCE_HEADER)),
				OperationalRequestSignatureSupport.prefix(request.getHeader(OperationalRequestSignatureSupport.BODY_SHA256_HEADER)),
				null
			);
		}
		return value.trim();
	}

	private long parseTimestamp(String timestampHeader, String keyId, String nonceFingerprint, String bodyHashPrefix) {
		try {
			return Long.parseLong(timestampHeader);
		} catch (NumberFormatException exception) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.INVALID_TIMESTAMP,
				keyId,
				nonceFingerprint,
				bodyHashPrefix,
				null
			);
		}
	}

	private void validateClockWindow(long timestamp, Instant now, String keyId, String nonceFingerprint, String bodyHashPrefix) {
		long nowEpochSeconds = now.getEpochSecond();
		if (Math.abs(nowEpochSeconds - timestamp) > properties.getMaxClockSkewSeconds()) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.TIMESTAMP_OUTSIDE_WINDOW,
				keyId,
				nonceFingerprint,
				bodyHashPrefix,
				null
			);
		}
	}

	private JsonNode parsePayload(byte[] requestBody, String keyId, String nonceFingerprint, String bodyHashPrefix) {
		try {
			JsonNode payload = objectMapper.readTree(requestBody);
			if (payload == null || !payload.isObject()) {
				throw new OperationalRequestValidationException(
					OperationalRequestRejectionReason.INVALID_JSON,
					keyId,
					nonceFingerprint,
					bodyHashPrefix,
					null
				);
			}
			return payload;
		} catch (Exception exception) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.INVALID_JSON,
				keyId,
				nonceFingerprint,
				bodyHashPrefix,
				null
			);
		}
	}

	private String extractSourceAccount(JsonNode payload) {
		JsonNode sourceAccount = payload.path("sourceAccount");
		return sourceAccount.isTextual() ? sourceAccount.asText() : null;
	}
}
