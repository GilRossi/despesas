package com.gilrossi.despesas.api.v1.driver;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.api.v1.driver.DriverModuleBootstrapResponse.DriverModuleProviderResponse;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;
import com.gilrossi.despesas.security.RequireDriverModule;

@Service
public class DriverModuleBootstrapService {

	private static final List<DriverModuleProviderResponse> SUPPORTED_PROVIDERS = List.of(
		new DriverModuleProviderResponse("UBER_DRIVER", "Uber Driver", "RIDE_HAILING"),
		new DriverModuleProviderResponse("APP99_DRIVER", "99 Motorista", "RIDE_HAILING"),
		new DriverModuleProviderResponse("INDRIVE", "Indrive", "RIDE_HAILING"),
		new DriverModuleProviderResponse("MOBIZAP_SP", "MobizapSP", "RIDE_HAILING"),
		new DriverModuleProviderResponse("IFOOD_DELIVERER", "iFood Entregador", "DELIVERY"),
		new DriverModuleProviderResponse("LALAMOVE", "Lalamove", "DELIVERY"),
		new DriverModuleProviderResponse("RAPPI_DELIVERER", "Rappi Entregador", "DELIVERY")
	);

	private final CurrentHouseholdProvider currentHouseholdProvider;

	public DriverModuleBootstrapService(CurrentHouseholdProvider currentHouseholdProvider) {
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	@RequireDriverModule
	public DriverModuleBootstrapResponse bootstrap() {
		return new DriverModuleBootstrapResponse(
			"DRIVER",
			currentHouseholdProvider.requireHouseholdId(),
			"Praia Grande",
			"SP",
			"BR",
			SUPPORTED_PROVIDERS
		);
	}
}
