package com.gilrossi.despesas.api.v1.driver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;
import com.gilrossi.despesas.security.RequireDriverModule;

@Service
public class DriverModuleProbeService {

	private final CurrentHouseholdProvider currentHouseholdProvider;

	public DriverModuleProbeService(CurrentHouseholdProvider currentHouseholdProvider) {
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	@RequireDriverModule
	public DriverModuleProbeResponse probe() {
		return new DriverModuleProbeResponse(
			"DRIVER",
			currentHouseholdProvider.requireHouseholdId(),
			true
		);
	}
}
