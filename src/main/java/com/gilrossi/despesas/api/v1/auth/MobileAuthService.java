package com.gilrossi.despesas.api.v1.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.gilrossi.despesas.identity.AuthResponse;
import com.gilrossi.despesas.security.ApiTokenService;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;

@Service
public class MobileAuthService {

	private final AuthenticationManager authenticationManager;
	private final ApiTokenService apiTokenService;

	public MobileAuthService(AuthenticationManager authenticationManager, ApiTokenService apiTokenService) {
		this.authenticationManager = authenticationManager;
		this.apiTokenService = apiTokenService;
	}

	public MobileAuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
		);
		if (!(authentication.getPrincipal() instanceof AuthenticatedHouseholdUser principal)) {
			throw new BadCredentialsException("Authentication failed");
		}
		return responseFor(principal);
	}

	public MobileAuthResponse refresh(RefreshTokenRequest request) {
		AuthenticatedHouseholdUser principal = apiTokenService.authenticateRefreshToken(request.refreshToken());
		return responseFor(principal);
	}

	private MobileAuthResponse responseFor(AuthenticatedHouseholdUser principal) {
		var accessToken = apiTokenService.issueAccessToken(principal);
		var refreshToken = apiTokenService.issueRefreshToken(principal);
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
