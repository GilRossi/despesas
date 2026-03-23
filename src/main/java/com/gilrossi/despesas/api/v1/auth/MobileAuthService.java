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

@Service
public class MobileAuthService {

	private final AuthenticationManager authenticationManager;
	private final ApiTokenService apiTokenService;
	private final RefreshTokenService refreshTokenService;

	public MobileAuthService(
		AuthenticationManager authenticationManager,
		ApiTokenService apiTokenService,
		RefreshTokenService refreshTokenService
	) {
		this.authenticationManager = authenticationManager;
		this.apiTokenService = apiTokenService;
		this.refreshTokenService = refreshTokenService;
	}

	public MobileAuthResponse login(LoginRequest request) {
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
