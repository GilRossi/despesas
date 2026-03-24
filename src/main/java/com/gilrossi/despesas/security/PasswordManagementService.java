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
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserNotFoundException;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;

@Service
public class PasswordManagementService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;
	private final SecurityAuditLogger securityAuditLogger;
	private final Clock clock;

	@Autowired
	public PasswordManagementService(
		AppUserRepository appUserRepository,
		PasswordEncoder passwordEncoder,
		RefreshTokenService refreshTokenService,
		SecurityAuditLogger securityAuditLogger
	) {
		this(
			appUserRepository,
			passwordEncoder,
			refreshTokenService,
			securityAuditLogger,
			Clock.systemUTC()
		);
	}

	PasswordManagementService(
		AppUserRepository appUserRepository,
		PasswordEncoder passwordEncoder,
		RefreshTokenService refreshTokenService,
		SecurityAuditLogger securityAuditLogger,
		Clock clock
	) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.securityAuditLogger = securityAuditLogger;
		this.clock = clock;
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
