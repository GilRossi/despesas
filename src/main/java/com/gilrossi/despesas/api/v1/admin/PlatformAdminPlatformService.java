package com.gilrossi.despesas.api.v1.admin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.Household;
import com.gilrossi.despesas.identity.HouseholdModule;
import com.gilrossi.despesas.identity.HouseholdModuleKey;
import com.gilrossi.despesas.identity.HouseholdModuleService;
import com.gilrossi.despesas.identity.HouseholdRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;

@Service
public class PlatformAdminPlatformService {

	private final HouseholdRepository householdRepository;
	private final AppUserRepository appUserRepository;
	private final HouseholdModuleService householdModuleService;
	private final HealthEndpoint healthEndpoint;
	private final InfoEndpoint infoEndpoint;
	private final Environment environment;

	public PlatformAdminPlatformService(
		HouseholdRepository householdRepository,
		AppUserRepository appUserRepository,
		HouseholdModuleService householdModuleService,
		HealthEndpoint healthEndpoint,
		InfoEndpoint infoEndpoint,
		Environment environment
	) {
		this.householdRepository = householdRepository;
		this.appUserRepository = appUserRepository;
		this.householdModuleService = householdModuleService;
		this.healthEndpoint = healthEndpoint;
		this.infoEndpoint = infoEndpoint;
		this.environment = environment;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public PlatformAdminPlatformOverviewResponse readOverview() {
		List<Household> households = householdRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();
		List<HouseholdModule> modules = householdModuleService.listForHouseholds(households.stream().map(Household::getId).toList());
		Map<HouseholdModuleKey, List<HouseholdModule>> modulesByKey = modules.stream()
			.collect(Collectors.groupingBy(HouseholdModule::getModuleKey));

		List<PlatformAdminPlatformOverviewResponse.ModuleUsage> moduleUsage = HouseholdModuleKey.supportedModules().stream()
			.map(moduleKey -> {
				List<HouseholdModule> moduleStates = modulesByKey.getOrDefault(moduleKey, List.of());
				long enabledSpaces = moduleStates.stream().filter(HouseholdModule::isEnabled).count();
				long disabledSpaces = households.size() - enabledSpaces;
				return new PlatformAdminPlatformOverviewResponse.ModuleUsage(
					moduleKey.name(),
					enabledSpaces,
					disabledSpaces,
					moduleKey.isMandatory()
				);
			})
			.toList();

		return new PlatformAdminPlatformOverviewResponse(
			households.size(),
			households.size(),
			appUserRepository.countByDeletedAtIsNull(),
			appUserRepository.countByPlatformRoleAndDeletedAtIsNull(PlatformUserRole.PLATFORM_ADMIN),
			moduleUsage,
			actuatorExposure()
		);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public PlatformAdminPlatformHealthResponse readHealth() {
		HealthComponent health = healthEndpoint.health();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();

		return new PlatformAdminPlatformHealthResponse(
			health.getStatus().getCode(),
			Instant.now(),
			new PlatformAdminPlatformHealthResponse.ActuatorExposure(
				isEndpointExposed("health"),
				isEndpointExposed("info"),
				isEndpointExposed("metrics")
			),
			new PlatformAdminPlatformHealthResponse.JvmSnapshot(
				Runtime.getRuntime().availableProcessors(),
				runtimeMXBean.getUptime(),
				heap.getUsed(),
				heap.getCommitted(),
				heap.getMax()
			),
			new PlatformAdminPlatformHealthResponse.SystemSnapshot(
				systemLoadAverage < 0 ? null : systemLoadAverage
			),
			infoEndpoint.info()
		);
	}

	private PlatformAdminPlatformOverviewResponse.ActuatorExposure actuatorExposure() {
		return new PlatformAdminPlatformOverviewResponse.ActuatorExposure(
			isEndpointExposed("health"),
			isEndpointExposed("info"),
			isEndpointExposed("metrics")
		);
	}

	private boolean isEndpointExposed(String endpointId) {
		String configuredEndpoints = environment.getProperty("management.endpoints.web.exposure.include", "");
		List<String> ids = List.of(configuredEndpoints.split(","));
		return ids.stream().map(String::trim).anyMatch(endpointId::equals);
	}
}
