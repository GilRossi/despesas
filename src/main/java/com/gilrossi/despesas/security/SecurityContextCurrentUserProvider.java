package com.gilrossi.despesas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

	@Override
	public AuthenticatedHouseholdUser requireCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedHouseholdUser principal)) {
			throw new IllegalStateException("Authenticated household user is required");
		}
		return principal;
	}
}
