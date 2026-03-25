package com.gilrossi.despesas.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.api.v1.admin.AdminPasswordResetRequest;
import com.gilrossi.despesas.api.v1.admin.AdminPasswordResetResponse;
import com.gilrossi.despesas.api.v1.auth.ChangePasswordRequest;
import com.gilrossi.despesas.api.v1.auth.ChangePasswordResponse;
import com.gilrossi.despesas.api.v1.auth.ForgotPasswordRequest;
import com.gilrossi.despesas.api.v1.auth.ForgotPasswordResponse;
import com.gilrossi.despesas.api.v1.auth.ResetPasswordRequest;
import com.gilrossi.despesas.api.v1.auth.ResetPasswordResponse;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserNotFoundException;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;
import com.gilrossi.despesas.security.ApiSecurityProperties;

@Service
public class PasswordManagementService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetTokenService passwordResetTokenService;
	private final RefreshTokenService refreshTokenService;
	private final SecurityAuditLogger securityAuditLogger;
	private final Clock clock;
	private final boolean exposeResetToken;

	@Autowired
	public PasswordManagementService(
		AppUserRepository appUserRepository,
		PasswordEncoder passwordEncoder,
		PasswordResetTokenService passwordResetTokenService,
		RefreshTokenService refreshTokenService,
		SecurityAuditLogger securityAuditLogger,
		ApiSecurityProperties securityProperties
	) {
		this(
			appUserRepository,
			passwordEncoder,
			passwordResetTokenService,
			refreshTokenService,
			securityAuditLogger,
			Clock.systemUTC(),
			securityProperties.exposeResetToken()
		);
	}

	PasswordManagementService(
		AppUserRepository appUserRepository,
		PasswordEncoder passwordEncoder,
		PasswordResetTokenService passwordResetTokenService,
		RefreshTokenService refreshTokenService,
		SecurityAuditLogger securityAuditLogger,
		Clock clock,
		boolean exposeResetToken
	) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordResetTokenService = passwordResetTokenService;
		this.refreshTokenService = refreshTokenService;
		this.securityAuditLogger = securityAuditLogger;
		this.clock = clock;
		this.exposeResetToken = exposeResetToken;
	}

	@Transactional
	public ChangePasswordResponse changeOwnPassword(AuthenticatedHouseholdUser principal, ChangePasswordRequest request) {
		validatePasswordConfirmation(request.newPassword(), request.newPasswordConfirmation());

		AppUser user = appUserRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
			.orElseThrow(() -> new AppUserNotFoundException("User not found"));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			securityAuditLogger.passwordChangeRejected(principal, "current_password_mismatch");
			throw new BadCredentialsException("Authentication failed");
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			securityAuditLogger.passwordChangeRejected(principal, "password_reuse");
			throw new IllegalArgumentException("New password must be different from current password");
		}

		Instant now = clock.instant();
		user.changePasswordHash(passwordEncoder.encode(request.newPassword()), now);
		int revokedRefreshTokens = refreshTokenService.revokeAllActiveForUser(
			user.getId(),
			RefreshTokenRevocationReason.PASSWORD_CHANGED
		);
		securityAuditLogger.passwordChangeSucceeded(principal, revokedRefreshTokens);
		return new ChangePasswordResponse(revokedRefreshTokens, true);
	}

	@Transactional
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public AdminPasswordResetResponse resetPassword(AuthenticatedHouseholdUser actor, AdminPasswordResetRequest request) {
		validatePasswordConfirmation(request.newPassword(), request.newPasswordConfirmation());

		String normalizedTargetEmail = normalizeEmail(request.targetEmail());
		AppUser targetUser = appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedTargetEmail)
			.orElseThrow(() -> {
				securityAuditLogger.passwordResetRejected(actor, normalizedTargetEmail, "target_not_found");
				return new AppUserNotFoundException("User not found");
			});
		if (targetUser.getPlatformRole() == PlatformUserRole.PLATFORM_ADMIN) {
			securityAuditLogger.passwordResetRejected(actor, normalizedTargetEmail, "platform_admin_target_blocked");
			throw new IllegalArgumentException("Platform admins must use self-service password change");
		}
		if (passwordEncoder.matches(request.newPassword(), targetUser.getPasswordHash())) {
			securityAuditLogger.passwordResetRejected(actor, normalizedTargetEmail, "password_reuse");
			throw new IllegalArgumentException("New password must be different from current password");
		}

		Instant now = clock.instant();
		targetUser.changePasswordHash(passwordEncoder.encode(request.newPassword()), now);
		int revokedRefreshTokens = refreshTokenService.revokeAllActiveForUser(
			targetUser.getId(),
			RefreshTokenRevocationReason.PASSWORD_RESET
		);
		securityAuditLogger.passwordResetSucceeded(actor, targetUser, revokedRefreshTokens);
		return new AdminPasswordResetResponse(maskEmail(targetUser.getEmail()), revokedRefreshTokens);
	}

	@Transactional
	public ForgotPasswordResponse issuePasswordResetToken(ForgotPasswordRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		AppUser targetUser = appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
			.orElse(null);
		if (targetUser == null) {
			return new ForgotPasswordResponse(maskEmail(normalizedEmail), null);
		}
		ApiIssuedToken resetToken = passwordResetTokenService.issue(targetUser.getId(), targetUser.getEmail());
		securityAuditLogger.passwordResetTokenIssued(normalizedEmail, resetToken.expiresAt());
		return new ForgotPasswordResponse(
			maskEmail(targetUser.getEmail()),
			exposeResetToken ? resetToken.value() : null
		);
	}

	@Transactional
	public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
		validatePasswordConfirmation(request.newPassword(), request.newPasswordConfirmation());

		PasswordResetTokenService.PasswordResetPayload payload = passwordResetTokenService.parse(request.token());
		AppUser user = appUserRepository.findByIdAndDeletedAtIsNull(payload.userId())
			.orElseThrow(() -> new BadCredentialsException("Reset token invalid"));
		if (!normalizeEmail(user.getEmail()).equals(normalizeEmail(payload.email()))) {
			throw new BadCredentialsException("Reset token invalid");
		}

		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			securityAuditLogger.passwordResetRejected(
				new AuthenticatedHouseholdUser(user.getId(), null, user.getPlatformRole().name(), user.getName(), user.getEmail(), "", clock.instant()),
				user.getEmail(),
				"password_reuse"
			);
			throw new IllegalArgumentException("New password must be different from current password");
		}

		Instant now = clock.instant();
		user.changePasswordHash(passwordEncoder.encode(request.newPassword()), now);
		int revokedRefreshTokens = refreshTokenService.revokeAllActiveForUser(
			user.getId(),
			RefreshTokenRevocationReason.PASSWORD_RESET
		);
		securityAuditLogger.passwordResetSucceeded(
			new AuthenticatedHouseholdUser(user.getId(), null, user.getPlatformRole().name(), user.getName(), user.getEmail(), "", now),
			user,
			revokedRefreshTokens
		);
		return new ResetPasswordResponse(revokedRefreshTokens, true);
	}

	private void validatePasswordConfirmation(String newPassword, String newPasswordConfirmation) {
		if (!newPassword.equals(newPasswordConfirmation)) {
			throw new IllegalArgumentException("New password confirmation does not match");
		}
	}

	private String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return "-";
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}
}
