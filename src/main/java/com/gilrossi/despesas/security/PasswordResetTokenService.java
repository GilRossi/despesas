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

public class PasswordResetTokenService {

	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
	private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);

	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final SecretKeySpec signingKey;

	public PasswordResetTokenService(ObjectMapper objectMapper, String secret) {
		this(objectMapper, Clock.systemUTC(), secret);
	}

	PasswordResetTokenService(ObjectMapper objectMapper, Clock clock, String secret) {
		this.objectMapper = objectMapper;
		this.clock = clock;
		this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public ApiIssuedToken issue(Long userId, String email) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(RESET_TOKEN_TTL);
		PasswordResetPayload payload = new PasswordResetPayload(
			userId,
			email,
			issuedAt.toEpochMilli(),
			expiresAt.getEpochSecond()
		);
		return new ApiIssuedToken(encode(payload), expiresAt);
	}

	public PasswordResetPayload parse(String token) {
		PasswordResetPayload payload = decode(token);
		if (payload.exp() <= clock.instant().getEpochSecond()) {
			throw new BadCredentialsException("Reset token expired");
		}
		return payload;
	}

	private String encode(PasswordResetPayload payload) {
		try {
			String encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
			String signature = URL_ENCODER.encodeToString(sign(encodedPayload));
			return encodedPayload + "." + signature;
		} catch (Exception exception) {
			throw new IllegalStateException("Could not issue reset token", exception);
		}
	}

	private PasswordResetPayload decode(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 2) {
				throw new BadCredentialsException("Invalid reset token");
			}
			byte[] providedSignature = URL_DECODER.decode(parts[1]);
			byte[] expectedSignature = sign(parts[0]);
			if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
				throw new BadCredentialsException("Invalid reset token");
			}
			byte[] payloadBytes = URL_DECODER.decode(parts[0]);
			return objectMapper.readValue(payloadBytes, PasswordResetPayload.class);
		} catch (BadCredentialsException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new BadCredentialsException("Invalid reset token", exception);
		}
	}

	private byte[] sign(String payload) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(signingKey);
		return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
	}

	public record PasswordResetPayload(Long userId, String email, long iat, long exp) {
	}
}
