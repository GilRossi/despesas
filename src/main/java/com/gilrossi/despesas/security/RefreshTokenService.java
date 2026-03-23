package com.gilrossi.despesas.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
	private static final int REFRESH_SECRET_BYTES = 32;
	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

	private final RefreshTokenRecordRepository refreshTokenRecordRepository;
	private final PasswordEncoder passwordEncoder;
	private final HouseholdUserDetailsService householdUserDetailsService;
	private final SecurityAuditLogger securityAuditLogger;
	private final Clock clock;
	private final SecureRandom secureRandom;

	@Autowired
	public RefreshTokenService(
		RefreshTokenRecordRepository refreshTokenRecordRepository,
		PasswordEncoder passwordEncoder,
		HouseholdUserDetailsService householdUserDetailsService,
		SecurityAuditLogger securityAuditLogger
	) {
		this(
			refreshTokenRecordRepository,
			passwordEncoder,
			householdUserDetailsService,
			securityAuditLogger,
			Clock.systemUTC(),
			new SecureRandom()
		);
	}

	RefreshTokenService(
		RefreshTokenRecordRepository refreshTokenRecordRepository,
		PasswordEncoder passwordEncoder,
		HouseholdUserDetailsService householdUserDetailsService,
		SecurityAuditLogger securityAuditLogger,
		Clock clock,
		SecureRandom secureRandom
	) {
		this.refreshTokenRecordRepository = refreshTokenRecordRepository;
		this.passwordEncoder = passwordEncoder;
		this.householdUserDetailsService = householdUserDetailsService;
		this.securityAuditLogger = securityAuditLogger;
		this.clock = clock;
		this.secureRandom = secureRandom;
	}

	@Transactional
	public ApiIssuedToken issueFor(AuthenticatedHouseholdUser principal) {
		Instant now = clock.instant();
		IssuedRefreshToken issued = issue(principal.getUserId(), UUID.randomUUID().toString(), now);
		return new ApiIssuedToken(issued.rawToken(), issued.expiresAt());
	}

	@Transactional(noRollbackFor = BadCredentialsException.class)
	public RefreshTokenRotationResult rotate(String rawRefreshToken) {
		ParsedRefreshToken parsedToken = parse(rawRefreshToken);
		RefreshTokenRecord current = loadTokenForUpdate(parsedToken.tokenId());
		validateSecret(parsedToken, current);

		Instant now = clock.instant();
		if (current.isExpired(now)) {
			current.revoke(RefreshTokenRevocationReason.EXPIRED, now);
			securityAuditLogger.refreshRejected(current.getTokenId(), current.getFamilyId(), current.getUserId(), "expired");
			throw invalidRefreshToken();
		}
		if (current.isRevoked()) {
			handleRevokedRefreshToken(current, now);
		}

		AuthenticatedHouseholdUser principal = loadActivePrincipal(current, now);
		IssuedRefreshToken replacement = issue(current.getUserId(), current.getFamilyId(), now);
		current.markRotated(replacement.tokenId(), now);
		securityAuditLogger.refreshSucceeded(principal, replacement.tokenId(), replacement.familyId());
		return new RefreshTokenRotationResult(
			principal,
			new ApiIssuedToken(replacement.rawToken(), replacement.expiresAt())
		);
	}

	@Transactional
	public void revokeFamilyForLogout(AuthenticatedHouseholdUser principal, String rawRefreshToken) {
		ParsedRefreshToken parsedToken = parse(rawRefreshToken);
		RefreshTokenRecord current = loadTokenForUpdate(parsedToken.tokenId());
		validateSecret(parsedToken, current);
		if (!principal.getUserId().equals(current.getUserId())) {
			throw new AccessDeniedException("Access denied");
		}

		int revokedTokens = revokeFamily(current.getFamilyId(), clock.instant(), RefreshTokenRevocationReason.LOGOUT);
		securityAuditLogger.logoutSucceeded(principal, current.getTokenId(), current.getFamilyId(), revokedTokens);
	}

	private IssuedRefreshToken issue(Long userId, String familyId, Instant now) {
		String tokenId = UUID.randomUUID().toString();
		String secret = generateSecret();
		Instant expiresAt = now.plus(REFRESH_TOKEN_TTL);
		refreshTokenRecordRepository.save(new RefreshTokenRecord(
			tokenId,
			familyId,
			userId,
			passwordEncoder.encode(secret),
			expiresAt
		));
		return new IssuedRefreshToken(tokenId, familyId, tokenId + "." + secret, expiresAt);
	}

	private RefreshTokenRecord loadTokenForUpdate(String tokenId) {
		return refreshTokenRecordRepository.findByTokenIdForUpdate(tokenId)
			.orElseThrow(() -> {
				securityAuditLogger.refreshRejected(tokenId, null, null, "token_not_found");
				return invalidRefreshToken();
			});
	}

	private void validateSecret(ParsedRefreshToken parsedToken, RefreshTokenRecord record) {
		if (!passwordEncoder.matches(parsedToken.secret(), record.getTokenHash())) {
			securityAuditLogger.refreshRejected(parsedToken.tokenId(), record.getFamilyId(), record.getUserId(), "secret_mismatch");
			throw invalidRefreshToken();
		}
	}

	private void handleRevokedRefreshToken(RefreshTokenRecord record, Instant now) {
		if (record.wasRotated()) {
			revokeFamily(record.getFamilyId(), now, RefreshTokenRevocationReason.REUSE_DETECTED);
			securityAuditLogger.refreshRejected(record.getTokenId(), record.getFamilyId(), record.getUserId(), "reuse_detected");
			throw invalidRefreshToken();
		}
		String reason = record.getRevocationReason() == null ? "revoked" : record.getRevocationReason().name().toLowerCase();
		securityAuditLogger.refreshRejected(record.getTokenId(), record.getFamilyId(), record.getUserId(), reason);
		throw invalidRefreshToken();
	}

	private AuthenticatedHouseholdUser loadActivePrincipal(RefreshTokenRecord record, Instant now) {
		try {
			return householdUserDetailsService.loadUserById(record.getUserId());
		} catch (UsernameNotFoundException exception) {
			revokeFamily(record.getFamilyId(), now, RefreshTokenRevocationReason.USER_STATE_INVALID);
			securityAuditLogger.refreshRejected(record.getTokenId(), record.getFamilyId(), record.getUserId(), "user_state_invalid");
			throw invalidRefreshToken();
		}
	}

	private int revokeFamily(String familyId, Instant now, RefreshTokenRevocationReason reason) {
		List<RefreshTokenRecord> activeTokens = refreshTokenRecordRepository.findByFamilyIdAndRevokedAtIsNull(familyId);
		for (RefreshTokenRecord token : activeTokens) {
			token.revoke(reason, now);
		}
		return activeTokens.size();
	}

	private ParsedRefreshToken parse(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw invalidRefreshToken();
		}
		String[] parts = rawRefreshToken.split("\\.", 2);
		if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
			throw invalidRefreshToken();
		}
		return new ParsedRefreshToken(parts[0].trim(), parts[1].trim());
	}

	private String generateSecret() {
		byte[] bytes = new byte[REFRESH_SECRET_BYTES];
		secureRandom.nextBytes(bytes);
		return URL_ENCODER.encodeToString(bytes);
	}

	private BadCredentialsException invalidRefreshToken() {
		return new BadCredentialsException("Authentication failed");
	}

	private record ParsedRefreshToken(String tokenId, String secret) {
	}

	private record IssuedRefreshToken(String tokenId, String familyId, String rawToken, Instant expiresAt) {
	}
}
