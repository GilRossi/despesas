package com.gilrossi.despesas.security;

import com.gilrossi.despesas.identity.AppUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.gilrossi.despesas.audit.PersistedAuditEventCategory;
import com.gilrossi.despesas.audit.PersistedAuditEventCommand;
import com.gilrossi.despesas.audit.PersistedAuditEventRecorder;
import com.gilrossi.despesas.audit.PersistedAuditEventStatus;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityAuditLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditLogger.class);
	private final PersistedAuditEventRecorder auditEventRecorder;

	public SecurityAuditLogger(PersistedAuditEventRecorder auditEventRecorder) {
		this.auditEventRecorder = auditEventRecorder;
	}

	public void loginSucceeded(AuthenticatedHouseholdUser principal) {
		LOGGER.info(
			"event=auth_login_success userId={} role={} householdId={} email={}",
			principal.getUserId(),
			principal.getRole(),
			principal.getHouseholdId(),
			maskEmail(principal.getUsername())
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_login_success", PersistedAuditEventStatus.SUCCESS)
			.userId(principal.getUserId())
			.householdId(principal.getHouseholdId())
			.actorRole(principal.getRole())
			.primaryReference(maskEmail(principal.getUsername()))
			.build());
	}

	public void loginFailed(String username, String reason) {
		LOGGER.warn(
			"event=auth_login_failure email={} reason={}",
			maskEmail(username),
			normalize(reason)
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_login_failure", PersistedAuditEventStatus.FAILURE)
			.primaryReference(maskEmail(username))
			.detailCode(normalize(reason))
			.build());
	}

	public void refreshSucceeded(AuthenticatedHouseholdUser principal, String tokenId, String familyId) {
		LOGGER.info(
			"event=auth_refresh_success userId={} role={} householdId={} tokenId={} familyId={}",
			principal.getUserId(),
			principal.getRole(),
			principal.getHouseholdId(),
			normalize(tokenId),
			normalize(familyId)
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_refresh_success", PersistedAuditEventStatus.SUCCESS)
			.userId(principal.getUserId())
			.householdId(principal.getHouseholdId())
			.actorRole(principal.getRole())
			.primaryReference(normalize(tokenId))
			.secondaryReference(normalize(familyId))
			.build());
	}

	public void refreshRejected(String tokenId, String familyId, Long userId, String reason) {
		LOGGER.warn(
			"event=auth_refresh_rejected userId={} tokenId={} familyId={} reason={}",
			userId,
			normalize(tokenId),
			normalize(familyId),
			normalize(reason)
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_refresh_rejected", PersistedAuditEventStatus.REJECTED)
			.userId(userId)
			.primaryReference(normalize(tokenId))
			.secondaryReference(normalize(familyId))
			.detailCode(normalize(reason))
			.build());
	}

	public void logoutSucceeded(AuthenticatedHouseholdUser principal, String tokenId, String familyId, int revokedTokens) {
		LOGGER.info(
			"event=auth_logout_success userId={} role={} householdId={} tokenId={} familyId={} revokedTokens={}",
			principal.getUserId(),
			principal.getRole(),
			principal.getHouseholdId(),
			normalize(tokenId),
			normalize(familyId),
			revokedTokens
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_logout_success", PersistedAuditEventStatus.SUCCESS)
			.userId(principal.getUserId())
			.householdId(principal.getHouseholdId())
			.actorRole(principal.getRole())
			.primaryReference(normalize(tokenId))
			.secondaryReference(normalize(familyId))
			.safeContext("revokedTokens", revokedTokens)
			.build());
	}

	public void passwordChangeSucceeded(AuthenticatedHouseholdUser principal, int revokedRefreshTokens) {
		LOGGER.info(
			"event=auth_password_change_success userId={} role={} householdId={} email={} revokedRefreshTokens={}",
			principal.getUserId(),
			principal.getRole(),
			principal.getHouseholdId(),
			maskEmail(principal.getUsername()),
			revokedRefreshTokens
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_password_change_success", PersistedAuditEventStatus.SUCCESS)
			.userId(principal.getUserId())
			.householdId(principal.getHouseholdId())
			.actorRole(principal.getRole())
			.primaryReference(maskEmail(principal.getUsername()))
			.safeContext("revokedRefreshTokens", revokedRefreshTokens)
			.build());
	}

	public void passwordChangeRejected(AuthenticatedHouseholdUser principal, String reason) {
		LOGGER.warn(
			"event=auth_password_change_rejected userId={} role={} householdId={} email={} reason={}",
			principal.getUserId(),
			principal.getRole(),
			principal.getHouseholdId(),
			maskEmail(principal.getUsername()),
			normalize(reason)
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_password_change_rejected", PersistedAuditEventStatus.REJECTED)
			.userId(principal.getUserId())
			.householdId(principal.getHouseholdId())
			.actorRole(principal.getRole())
			.primaryReference(maskEmail(principal.getUsername()))
			.detailCode(normalize(reason))
			.build());
	}

	public void passwordResetSucceeded(AuthenticatedHouseholdUser actor, AppUser targetUser, int revokedRefreshTokens) {
		LOGGER.info(
			"event=auth_password_reset_success actorUserId={} actorRole={} actorHouseholdId={} targetEmail={} revokedRefreshTokens={}",
			actor.getUserId(),
			actor.getRole(),
			actor.getHouseholdId(),
			maskEmail(targetUser.getEmail()),
			revokedRefreshTokens
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_password_reset_success", PersistedAuditEventStatus.SUCCESS)
			.userId(actor.getUserId())
			.householdId(actor.getHouseholdId())
			.actorRole(actor.getRole())
			.primaryReference(maskEmail(targetUser.getEmail()))
			.secondaryReference(maskEmail(actor.getUsername()))
			.safeContext("revokedRefreshTokens", revokedRefreshTokens)
			.build());
	}

	public void passwordResetRejected(AuthenticatedHouseholdUser actor, String targetEmail, String reason) {
		LOGGER.warn(
			"event=auth_password_reset_rejected actorUserId={} actorRole={} actorHouseholdId={} targetEmail={} reason={}",
			actor.getUserId(),
			actor.getRole(),
			actor.getHouseholdId(),
			maskEmail(targetEmail),
			normalize(reason)
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_password_reset_rejected", PersistedAuditEventStatus.REJECTED)
			.userId(actor.getUserId())
			.householdId(actor.getHouseholdId())
			.actorRole(actor.getRole())
			.primaryReference(maskEmail(targetEmail))
			.secondaryReference(maskEmail(actor.getUsername()))
			.detailCode(normalize(reason))
			.build());
	}

	public void loginRateLimited(String username, RateLimitExceededException exception) {
		LOGGER.warn(
			"event=auth_login_rate_limited email={} retryAfterSeconds={} maxRequests={} windowSeconds={}",
			maskEmail(username),
			exception.getRetryAfterSeconds(),
			exception.getMaxRequests(),
			exception.getWindowSeconds()
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_login_rate_limited", PersistedAuditEventStatus.LIMITED)
			.primaryReference(maskEmail(username))
			.detailCode(exception.getScope().name())
			.safeContext("retryAfterSeconds", exception.getRetryAfterSeconds())
			.safeContext("maxRequests", exception.getMaxRequests())
			.safeContext("windowSeconds", exception.getWindowSeconds())
			.build());
	}

	public void refreshRateLimited(Long userId, String familyId, RateLimitExceededException exception) {
		LOGGER.warn(
			"event=auth_refresh_rate_limited userId={} familyId={} retryAfterSeconds={} maxRequests={} windowSeconds={}",
			userId,
			normalize(familyId),
			exception.getRetryAfterSeconds(),
			exception.getMaxRequests(),
			exception.getWindowSeconds()
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.AUTH, "auth_refresh_rate_limited", PersistedAuditEventStatus.LIMITED)
			.userId(userId)
			.secondaryReference(normalize(familyId))
			.detailCode(exception.getScope().name())
			.safeContext("retryAfterSeconds", exception.getRetryAfterSeconds())
			.safeContext("maxRequests", exception.getMaxRequests())
			.safeContext("windowSeconds", exception.getWindowSeconds())
			.build());
	}

	public void accessDenied(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Object principal = authentication == null ? null : authentication.getPrincipal();
		if (principal instanceof AuthenticatedHouseholdUser user) {
			LOGGER.warn(
				"event=access_denied method={} path={} userId={} role={} householdId={}",
				request.getMethod(),
				request.getRequestURI(),
				user.getUserId(),
				user.getRole(),
				user.getHouseholdId()
			);
			auditEventRecorder.recordSafely(PersistedAuditEventCommand
				.event(PersistedAuditEventCategory.ACCESS, "access_denied", PersistedAuditEventStatus.DENIED)
				.userId(user.getUserId())
				.householdId(user.getHouseholdId())
				.actorRole(user.getRole())
				.requestMethod(request.getMethod())
				.requestPath(request.getRequestURI())
				.build());
			return;
		}
		LOGGER.warn(
			"event=access_denied method={} path={} principal={}",
			request.getMethod(),
			request.getRequestURI(),
			authentication == null ? "anonymous" : normalize(authentication.getName())
		);
		auditEventRecorder.recordSafely(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.ACCESS, "access_denied", PersistedAuditEventStatus.DENIED)
			.requestMethod(request.getMethod())
			.requestPath(request.getRequestURI())
			.primaryReference(authentication == null ? "anonymous" : normalize(authentication.getName()))
			.build());
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

	private String normalize(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
