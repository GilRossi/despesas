package com.gilrossi.despesas.api.v1.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserNotFoundException;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.AuthResponse;
import com.gilrossi.despesas.identity.OnboardingStatusResponse;
import com.gilrossi.despesas.security.ApiIssuedToken;
import com.gilrossi.despesas.security.ApiTokenService;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;
import com.gilrossi.despesas.security.PasswordManagementService;
import com.gilrossi.despesas.security.PasswordResetTokenService;
import com.gilrossi.despesas.security.RefreshTokenRotationResult;
import com.gilrossi.despesas.security.RefreshTokenService;
import com.gilrossi.despesas.security.SecurityAuditLogger;
import com.gilrossi.despesas.ratelimit.AbuseProtectionService;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;

@Service
public class MobileAuthService {

	private final AuthenticationManager authenticationManager;
	private final AppUserRepository appUserRepository;
	private final ApiTokenService apiTokenService;
	private final PasswordResetTokenService passwordResetTokenService;
	private final RefreshTokenService refreshTokenService;
	private final PasswordManagementService passwordManagementService;
	private final AbuseProtectionService abuseProtectionService;
	private final SecurityAuditLogger securityAuditLogger;

	public MobileAuthService(
		AuthenticationManager authenticationManager,
		AppUserRepository appUserRepository,
		ApiTokenService apiTokenService,
		PasswordResetTokenService passwordResetTokenService,
		RefreshTokenService refreshTokenService,
		PasswordManagementService passwordManagementService,
		AbuseProtectionService abuseProtectionService,
		SecurityAuditLogger securityAuditLogger
	) {
		this.authenticationManager = authenticationManager;
		this.appUserRepository = appUserRepository;
		this.apiTokenService = apiTokenService;
		this.passwordResetTokenService = passwordResetTokenService;
		this.refreshTokenService = refreshTokenService;
		this.passwordManagementService = passwordManagementService;
		this.abuseProtectionService = abuseProtectionService;
		this.securityAuditLogger = securityAuditLogger;
	}

	public MobileAuthResponse login(LoginRequest request) {
		try {
			Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
			);
			if (!(authentication.getPrincipal() instanceof AuthenticatedHouseholdUser principal)) {
				throw new BadCredentialsException("Authentication failed");
			}
			abuseProtectionService.clearAuthLoginFailures(request.email());
			return responseFor(principal, refreshTokenService.issueFor(principal));
		} catch (AuthenticationException exception) {
			try {
				abuseProtectionService.registerAuthLoginFailure(request.email());
			} catch (RateLimitExceededException rateLimitException) {
				securityAuditLogger.loginRateLimited(request.email(), rateLimitException);
				throw rateLimitException;
			}
			throw exception;
		}
	}

	public MobileAuthResponse refresh(RefreshTokenRequest request) {
		RefreshTokenRotationResult rotationResult = refreshTokenService.rotate(request.refreshToken());
		return responseFor(rotationResult.principal(), rotationResult.refreshToken());
	}

	public void logout(AuthenticatedHouseholdUser principal, LogoutRequest request) {
		refreshTokenService.revokeFamilyForLogout(principal, request.refreshToken());
	}

	public ChangePasswordResponse changePassword(AuthenticatedHouseholdUser principal, ChangePasswordRequest request) {
		return passwordManagementService.changeOwnPassword(principal, request);
	}

	public ForgotPasswordResponse issuePasswordResetToken(ForgotPasswordRequest request) {
		try {
			abuseProtectionService.checkAuthLogin(request.email());
		} catch (RateLimitExceededException exception) {
			securityAuditLogger.loginRateLimited(request.email(), exception);
			throw exception;
		}
		return passwordManagementService.issuePasswordResetToken(request);
	}

	public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
		return passwordManagementService.resetPassword(request);
	}

	public AuthResponse currentUser(AuthenticatedHouseholdUser principal) {
		return buildAuthResponse(principal);
	}

	private MobileAuthResponse responseFor(AuthenticatedHouseholdUser principal, ApiIssuedToken refreshToken) {
		var accessToken = apiTokenService.issueAccessToken(principal);
		return new MobileAuthResponse(
			"Bearer",
			accessToken.value(),
			accessToken.expiresAt(),
			refreshToken.value(),
			refreshToken.expiresAt(),
			buildAuthResponse(principal)
		);
	}

	private AuthResponse buildAuthResponse(AuthenticatedHouseholdUser principal) {
		AppUser user = appUserRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
			.orElseThrow(() -> new AppUserNotFoundException("User not found"));
		return new AuthResponse(
			principal.getUserId(),
			principal.getHouseholdId(),
			principal.getUsername(),
			principal.getDisplayName(),
			principal.getRole(),
			OnboardingStatusResponse.from(user)
		);
	}
}
