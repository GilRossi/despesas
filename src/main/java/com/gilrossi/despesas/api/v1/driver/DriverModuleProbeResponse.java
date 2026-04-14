package com.gilrossi.despesas.api.v1.driver;

public record DriverModuleProbeResponse(
	String moduleKey,
	Long spaceId,
	boolean enabled
) {
}
