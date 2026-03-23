package com.gilrossi.despesas.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuthenticationEvents {

	private final SecurityAuditLogger securityAuditLogger;

	public SecurityAuthenticationEvents(SecurityAuditLogger securityAuditLogger) {
		this.securityAuditLogger = securityAuditLogger;
	}

	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof AuthenticatedHouseholdUser user) {
			securityAuditLogger.loginSucceeded(user);
		}
	}

	@EventListener
	public void onFailure(AbstractAuthenticationFailureEvent event) {
		Object principal = event.getAuthentication() == null ? null : event.getAuthentication().getPrincipal();
		securityAuditLogger.loginFailed(
			principal == null ? null : principal.toString(),
			event.getException().getClass().getSimpleName()
		);
	}
}
