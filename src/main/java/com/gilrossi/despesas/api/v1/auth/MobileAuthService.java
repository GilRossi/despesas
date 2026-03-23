package com.gilrossi.despesas.api.v1.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.gilrossi.despesas.identity.AuthResponse;
import com.gilrossi.despesas.security.ApiIssuedToken;
import com.gilrossi.despesas.security.ApiTokenService;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;
import com.gilrossi.despesas.security.RefreshTokenRotationResult;
import com.gilrossi.despesas.security.RefreshTokenService;
import com.gilrossi.despesas.security.SecurityAuditLogger;
import com.gilrossi.despesas.ratelimit.AbuseProtectionService;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;

@Service
public class MobileAuthService {

	private final AuthenticationManager authenticationManager;
	private final ApiTokenService apiTokenService;
	private final RefreshTokenService refreshTokenService;
	private final AbuseProtectionService abuseProtectionService;
	private final SecurityAuditLogger securityAuditLogger;

	public MobileAuthService(
		AuthenticationManager authenticationManager,
		ApiTokenService apiTokenService,
		RefreshTokenService refreshTokenService,
		AbuseProtectionService abuseProtectionService,
		SecurityAuditLogger securityAuditLogger
	) {
		this.authenticationManager = authenticationManager;
		this.apiTokenService = apiTokenService;
		this.refreshTokenService = refreshTokenService;
		this.abuseProtectionService = abuseProtectionService;
		this.securityAuditLogger = securityAuditLogger;
	}

	public MobileAuthResponse login(LoginRequest request) {
		try {
			abuseProtectionService.checkAuthLogin(request.email());
		} catch (RateLimitExceededException exception) {
			securityAuditLogger.loginRateLimited(request.email(), exception);
			throw exception;
		}
		Authentication authentication = authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
		);
		if (!(authentication.getPrincipal() instanceof AuthenticatedHouseholdUser principal)) {
			throw new BadCredentialsException("Authentication failed");
		}
		return responseFor(principal, refreshTokenService.issueFor(principal));
	}

	public MobileAuthResponse refresh(RefreshTokenRequest request) {
		RefreshTokenRotationResult rotationResult = refreshTokenService.rotate(request.refreshToken());
		return responseFor(rotationResult.principal(), rotationResult.refreshToken());
	}

	public void logout(AuthenticatedHouseholdUser principal, LogoutRequest request) {
		refreshTokenService.revokeFamilyForLogout(principal, request.refreshToken());
	}

	private MobileAuthResponse responseFor(AuthenticatedHouseholdUser principal, ApiIssuedToken refreshToken) {
		var accessToken = apiTokenService.issueAccessToken(principal);
		return new MobileAuthResponse(
			"Bearer",
			accessToken.value(),
			accessToken.expiresAt(),
			refreshToken.value(),
			refreshToken.expiresAt(),
			new AuthResponse(
				principal.getUserId(),
				principal.getHouseholdId(),
				principal.getUsername(),
				principal.getDisplayName(),
				principal.getRole()
			)
		);
	}
}
