package com.gilrossi.despesas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.authentication.BadCredentialsException;

import com.fasterxml.jackson.databind.ObjectMapper;
public class ApiTokenService {

	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final SecretKeySpec signingKey;

	public ApiTokenService(ObjectMapper objectMapper, String secret) {
		this(objectMapper, Clock.systemUTC(), secret);
	}

	ApiTokenService(ObjectMapper objectMapper, Clock clock, String secret) {
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public ApiIssuedToken issueAccessToken(AuthenticatedHouseholdUser principal) {
		return issue(principal, TokenType.ACCESS, ACCESS_TOKEN_TTL);
	}

	public ApiIssuedToken issueRefreshToken(AuthenticatedHouseholdUser principal) {
		return issue(principal, TokenType.REFRESH, REFRESH_TOKEN_TTL);
	}

	public AuthenticatedApiToken authenticateAccessToken(String token) {
		return parse(token, TokenType.ACCESS);
	}

	public AuthenticatedHouseholdUser authenticateRefreshToken(String token) {
		return parse(token, TokenType.REFRESH).principal();
	}

	private ApiIssuedToken issue(AuthenticatedHouseholdUser principal, TokenType type, Duration ttl) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(ttl);
		ApiTokenPayload payload = new ApiTokenPayload(
			type.name(),
			principal.getUserId(),
			principal.getHouseholdId(),
			principal.getRole(),
			principal.getDisplayName(),
			principal.getUsername(),
			issuedAt.toEpochMilli(),
			expiresAt.getEpochSecond()
		);
		return new ApiIssuedToken(encode(payload), expiresAt);
	}

	private AuthenticatedApiToken parse(String token, TokenType expectedType) {
		ApiTokenPayload payload = decode(token);
		if (!expectedType.name().equals(payload.type())) {
			throw new BadCredentialsException("Authentication failed");
		}
		if (payload.exp() <= clock.instant().getEpochSecond()) {
			throw new BadCredentialsException("Authentication failed");
		}
		AuthenticatedHouseholdUser principal = new AuthenticatedHouseholdUser(
			payload.userId(),
			payload.householdId(),
			payload.role(),
			payload.name(),
			payload.email(),
			"",
			Instant.ofEpochMilli(payload.iat())
		);
		return new AuthenticatedApiToken(principal, Instant.ofEpochMilli(payload.iat()));
	}

	private String encode(ApiTokenPayload payload) {
		try {
			String encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
			String signature = URL_ENCODER.encodeToString(sign(encodedPayload));
			return encodedPayload + "." + signature;
		} catch (Exception exception) {
			throw new IllegalStateException("Could not issue API token", exception);
		}
	}

	private ApiTokenPayload decode(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 2) {
				throw new BadCredentialsException("Authentication failed");
			}
			byte[] providedSignature = URL_DECODER.decode(parts[1]);
			byte[] expectedSignature = sign(parts[0]);
			if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
				throw new BadCredentialsException("Authentication failed");
			}
			byte[] payloadBytes = URL_DECODER.decode(parts[0]);
			return objectMapper.readValue(payloadBytes, ApiTokenPayload.class);
		} catch (BadCredentialsException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new BadCredentialsException("Authentication failed", exception);
		}
	}

	private byte[] sign(String payload) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(signingKey);
		return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
	}

	private enum TokenType {
		ACCESS,
		REFRESH
	}

	public record AuthenticatedApiToken(AuthenticatedHouseholdUser principal, Instant issuedAt) {
	}
}
