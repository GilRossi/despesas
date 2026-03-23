package com.gilrossi.despesas.financialassistant;

import org.springframework.stereotype.Component;

import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;
import com.gilrossi.despesas.security.CurrentUserProvider;

@Component
public class FinancialAssistantAccessContextProvider {

	private final CurrentUserProvider currentUserProvider;

	public FinancialAssistantAccessContextProvider(CurrentUserProvider currentUserProvider) {
		this.currentUserProvider = currentUserProvider;
	}

	public FinancialAssistantAccessContext requireContext() {
		AuthenticatedHouseholdUser currentUser = currentUserProvider.requireCurrentUser();
		if (currentUser.getHouseholdId() == null || "PLATFORM_ADMIN".equals(currentUser.getRole())) {
			throw FinancialAssistantContextException.invalidHouseholdScope(currentUser.getUserId(), currentUser.getRole());
		}
		return new FinancialAssistantAccessContext(
			currentUser.getUserId(),
			currentUser.getHouseholdId(),
			currentUser.getRole()
		);
	}
}
