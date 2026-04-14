package com.gilrossi.despesas.identity;

import java.util.EnumSet;
import java.util.Set;

public enum HouseholdModuleKey {
	FINANCIAL(true),
	DRIVER(false);

	private final boolean mandatory;

	HouseholdModuleKey(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public static Set<HouseholdModuleKey> supportedModules() {
		return EnumSet.allOf(HouseholdModuleKey.class);
	}

	public static Set<HouseholdModuleKey> defaultEnabledModules() {
		return EnumSet.of(FINANCIAL);
	}
}
