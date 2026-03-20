package com.gilrossi.despesas.security;

public interface CurrentUserProvider {

	AuthenticatedHouseholdUser requireCurrentUser();
}
