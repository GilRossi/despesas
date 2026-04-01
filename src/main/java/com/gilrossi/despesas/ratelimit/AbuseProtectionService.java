package com.gilrossi.despesas.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.gilrossi.despesas.financialassistant.FinancialAssistantAccessContext;

@Service
public class AbuseProtectionService {

	private final RateLimitService rateLimitService;
	private final RateLimitProperties properties;

	public AbuseProtectionService(RateLimitService rateLimitService, RateLimitProperties properties) {
		this.rateLimitService = rateLimitService;
		this.properties = properties;
	}

	public void checkAuthLogin(String email) {
		enforce(RateLimitScope.AUTH_LOGIN, fingerprint(email), properties.getAuthLogin());
	}

	public void registerAuthLoginFailure(String email) {
		enforce(RateLimitScope.AUTH_LOGIN, fingerprint(email), properties.getAuthLogin());
	}

	public void clearAuthLoginFailures(String email) {
		rateLimitService.reset(RateLimitScope.AUTH_LOGIN, fingerprint(email));
	}

	public void checkAuthRefresh(Long userId, String familyId) {
		String key = userId == null ? fingerprint(familyId) : userId + ":" + normalize(familyId);
		enforce(RateLimitScope.AUTH_REFRESH, key, properties.getAuthRefresh());
	}

	public void checkAssistantQuery(FinancialAssistantAccessContext context) {
		enforce(
			RateLimitScope.ASSISTANT_QUERY,
			context.userId() + ":" + context.householdId(),
			properties.getAssistantQuery()
		);
	}

	public void checkOperationalEmailIngestion(String keyId, String requestPath) {
		enforce(
			RateLimitScope.OPERATIONAL_EMAIL_INGESTION,
			normalize(keyId) + ":" + normalize(requestPath),
			properties.getOperationalEmailIngestion()
		);
	}

	private void enforce(RateLimitScope scope, String scopeKey, RateLimitProperties.Rule rule) {
		rateLimitService.enforce(scope, scopeKey, rule.getMaxRequests(), rule.getWindowSeconds());
	}

	private String fingerprint(String value) {
		String normalized = normalize(value).toLowerCase(Locale.ROOT);
		byte[] hash = sha256(normalized);
		return toHexPrefix(hash, 12);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? "-" : value.trim();
	}

	private byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is required", exception);
		}
	}

	private String toHexPrefix(byte[] bytes, int maxBytes) {
		StringBuilder builder = new StringBuilder(maxBytes * 2);
		for (int index = 0; index < bytes.length && index < maxBytes; index++) {
			builder.append(String.format("%02x", bytes[index]));
		}
		return builder.toString();
	}
}
