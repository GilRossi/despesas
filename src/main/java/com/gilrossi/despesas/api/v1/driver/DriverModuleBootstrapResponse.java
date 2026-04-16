package com.gilrossi.despesas.api.v1.driver;

import java.util.List;

public record DriverModuleBootstrapResponse(
	String moduleKey,
	Long spaceId,
	String targetCity,
	String targetState,
	String targetCountry,
	List<DriverModuleProviderResponse> providers
) {

	public record DriverModuleProviderResponse(
		String key,
		String label,
		String category
	) {
	}
}
