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
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.HouseholdModule;
import com.gilrossi.despesas.identity.HouseholdModuleKey;
import com.gilrossi.despesas.identity.HouseholdModuleService;
import com.gilrossi.despesas.identity.HouseholdRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;

@Service
public class PlatformAdminPlatformService {

	private final HouseholdRepository householdRepository;
	private final HouseholdMemberRepository householdMemberRepository;
	private final AppUserRepository appUserRepository;
	private final HouseholdModuleService householdModuleService;
	private final PlatformAdminOperationalAlertEvaluator operationalAlertEvaluator;
	private final HealthEndpoint healthEndpoint;
	private final InfoEndpoint infoEndpoint;
	private final Environment environment;

	public PlatformAdminPlatformService(
		HouseholdRepository householdRepository,
		HouseholdMemberRepository householdMemberRepository,
		AppUserRepository appUserRepository,
		HouseholdModuleService householdModuleService,
		PlatformAdminOperationalAlertEvaluator operationalAlertEvaluator,
		HealthEndpoint healthEndpoint,
		InfoEndpoint infoEndpoint,
		Environment environment
	) {
		this.householdRepository = householdRepository;
		this.householdMemberRepository = householdMemberRepository;
		this.appUserRepository = appUserRepository;
		this.householdModuleService = householdModuleService;
		this.operationalAlertEvaluator = operationalAlertEvaluator;
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
		Map<String, Object> info = infoEndpoint.info();
		PlatformAdminPlatformHealthResponse.ActuatorExposure actuatorExposure =
			new PlatformAdminPlatformHealthResponse.ActuatorExposure(
				isEndpointExposed("health"),
				isEndpointExposed("info"),
				isEndpointExposed("metrics")
			);
		long spacesWithoutOwner = countSpacesWithoutOwner();

		return new PlatformAdminPlatformHealthResponse(
			health.getStatus().getCode(),
			Instant.now(),
			actuatorExposure,
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
			info,
			operationalAlertEvaluator.evaluate(new PlatformAdminOperationalAlertEvaluator.Input(
				health.getStatus().getCode(),
				actuatorExposure.healthExposed(),
				actuatorExposure.infoExposed(),
				actuatorExposure.metricsExposed(),
				info.isEmpty(),
				Runtime.getRuntime().availableProcessors(),
				heap.getUsed(),
				heap.getMax(),
				systemLoadAverage < 0 ? null : systemLoadAverage,
				spacesWithoutOwner
			))
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

	private long countSpacesWithoutOwner() {
		List<Household> households = householdRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();
		if (households.isEmpty()) {
			return 0;
		}

		Map<Long, List<HouseholdMember>> membershipsByHouseholdId = householdMemberRepository
			.findActiveMembershipsByHouseholdIds(households.stream().map(Household::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(member -> member.getHousehold().getId()));

		return households.stream()
			.filter(household -> membershipsByHouseholdId
				.getOrDefault(household.getId(), List.of())
				.stream()
				.noneMatch(member -> member.getRole() == HouseholdMemberRole.OWNER))
			.count();
	}
}
