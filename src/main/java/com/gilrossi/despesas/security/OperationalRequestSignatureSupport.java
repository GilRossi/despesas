package com.gilrossi.despesas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class OperationalRequestSignatureSupport {

	public static final String KEY_ID_HEADER = "X-Operational-Key-Id";
	public static final String TIMESTAMP_HEADER = "X-Operational-Timestamp";
	public static final String NONCE_HEADER = "X-Operational-Nonce";
	public static final String BODY_SHA256_HEADER = "X-Operational-Body-SHA256";
	public static final String SIGNATURE_HEADER = "X-Operational-Signature";
	public static final String SIGNATURE_VERSION = "v1";

	private static final HexFormat HEX = HexFormat.of();

	private OperationalRequestSignatureSupport() {
	}

	public static String sha256Hex(byte[] value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HEX.formatHex(digest.digest(value));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public static String hmacSha256Hex(String secret, String canonicalPayload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HEX.formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to calculate HMAC-SHA256 signature", exception);
		}
	}

	public static String canonicalPayload(
		String method,
		String requestPath,
		String keyId,
		String timestamp,
		String nonce,
		String bodySha256
	) {
		return String.join(
			"\n",
			SIGNATURE_VERSION,
			normalize(method).toUpperCase(Locale.ROOT),
			normalize(requestPath),
			normalize(keyId),
			normalize(timestamp),
			normalize(nonce),
			normalize(bodySha256).toLowerCase(Locale.ROOT)
		);
	}

	public static boolean constantTimeEquals(String expected, String provided) {
		return MessageDigest.isEqual(
			normalize(expected).getBytes(StandardCharsets.UTF_8),
			normalize(provided).getBytes(StandardCharsets.UTF_8)
		);
	}

	public static String fingerprint(String value) {
		String normalized = normalize(value);
		if (normalized.isBlank()) {
			return "-";
		}
		return sha256Hex(normalized.getBytes(StandardCharsets.UTF_8)).substring(0, 12);
	}

	public static String prefix(String value) {
		String normalized = normalize(value);
		if (normalized.isBlank()) {
			return "-";
		}
		return normalized.substring(0, Math.min(12, normalized.length()));
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}
}
