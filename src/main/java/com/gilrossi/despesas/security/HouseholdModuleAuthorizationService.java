package com.gilrossi.despesas.security;

import org.springframework.stereotype.Component;

import com.gilrossi.despesas.identity.HouseholdModuleKey;
import com.gilrossi.despesas.identity.HouseholdModuleService;

@Component("householdModuleAuthorization")
public class HouseholdModuleAuthorizationService {

	private final CurrentUserProvider currentUserProvider;
	private final HouseholdModuleService householdModuleService;

	public HouseholdModuleAuthorizationService(
		CurrentUserProvider currentUserProvider,
		HouseholdModuleService householdModuleService
	) {
		this.currentUserProvider = currentUserProvider;
		this.householdModuleService = householdModuleService;
	}

	public boolean canAccess(HouseholdModuleKey moduleKey) {
		if (moduleKey == null) {
			return false;
		}
		try {
			AuthenticatedHouseholdUser currentUser = currentUserProvider.requireCurrentUser();
			Long householdId = currentUser.getHouseholdId();
			if (householdId == null) {
				return false;
			}
			return householdModuleService.isEnabled(householdId, moduleKey);
		} catch (IllegalStateException exception) {
			return false;
		}
	}
}
