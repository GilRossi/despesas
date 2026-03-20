package com.gilrossi.despesas.security;

import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentHouseholdProvider implements CurrentHouseholdProvider {

	private final CurrentUserProvider currentUserProvider;

	public SecurityContextCurrentHouseholdProvider(CurrentUserProvider currentUserProvider) {
		this.currentUserProvider = currentUserProvider;
	}

	@Override
	public Long requireHouseholdId() {
		return currentUserProvider.requireCurrentUser().getHouseholdId();
	}
}
