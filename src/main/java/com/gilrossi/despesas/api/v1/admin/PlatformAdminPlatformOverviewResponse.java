package com.gilrossi.despesas.api.v1.admin;

import java.util.List;

public record PlatformAdminPlatformOverviewResponse(
	long totalSpaces,
	long activeSpaces,
	long totalUsers,
	long totalPlatformAdmins,
	List<ModuleUsage> modules,
	ActuatorExposure actuator
) {

	public record ModuleUsage(
		String key,
		long enabledSpaces,
		long disabledSpaces,
		boolean mandatory
	) {
	}

	public record ActuatorExposure(
		boolean healthExposed,
		boolean infoExposed,
		boolean metricsExposed
	) {
	}
}
