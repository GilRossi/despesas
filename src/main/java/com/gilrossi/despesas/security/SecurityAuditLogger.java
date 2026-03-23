package com.gilrossi.despesas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityAuditLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditLogger.class);

	public void loginSucceeded(AuthenticatedHouseholdUser principal) {
		LOGGER.info(
			"event=auth_login_success userId={} role={} householdId={} email={}",
			principal.getUserId(),
			principal.getRole(),
			principal.getHouseholdId(),
			maskEmail(principal.getUsername())
		);
	}

	public void loginFailed(String username, String reason) {
		LOGGER.warn(
			"event=auth_login_failure email={} reason={}",
			maskEmail(username),
			normalize(reason)
		);
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
	}

	public void refreshRejected(String tokenId, String familyId, Long userId, String reason) {
		LOGGER.warn(
			"event=auth_refresh_rejected userId={} tokenId={} familyId={} reason={}",
			userId,
			normalize(tokenId),
			normalize(familyId),
			normalize(reason)
		);
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
			return;
		}
		LOGGER.warn(
			"event=access_denied method={} path={} principal={}",
			request.getMethod(),
			request.getRequestURI(),
			authentication == null ? "anonymous" : normalize(authentication.getName())
		);
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
