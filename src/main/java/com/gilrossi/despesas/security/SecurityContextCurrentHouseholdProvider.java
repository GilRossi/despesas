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
		Long householdId = currentUserProvider.requireCurrentUser().getHouseholdId();
		if (householdId == null) {
			throw new IllegalStateException("Authenticated household membership is required");
		}
		return householdId;
	}
}
