package com.gilrossi.despesas.api.v1.admin;

import java.util.List;

import com.gilrossi.despesas.identity.HouseholdModuleKey;

public record UpdateSpaceModulesRequest(
	List<HouseholdModuleKey> enabledModules
) {
}
