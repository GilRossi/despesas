package com.gilrossi.despesas.api.v1.admin;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.identity.Household;
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.HouseholdModule;
import com.gilrossi.despesas.identity.HouseholdModuleKey;
import com.gilrossi.despesas.identity.HouseholdModuleService;
import com.gilrossi.despesas.identity.HouseholdRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

@Service
public class PlatformAdminSpaceService {

	private final RegistrationService registrationService;
	private final HouseholdRepository householdRepository;
	private final HouseholdMemberRepository householdMemberRepository;
	private final HouseholdModuleService householdModuleService;

	public PlatformAdminSpaceService(
		RegistrationService registrationService,
		HouseholdRepository householdRepository,
		HouseholdMemberRepository householdMemberRepository,
		HouseholdModuleService householdModuleService
	) {
		this.registrationService = registrationService;
		this.householdRepository = householdRepository;
		this.householdMemberRepository = householdMemberRepository;
		this.householdModuleService = householdModuleService;
	}

	@Transactional
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public PlatformAdminSpaceResponse create(CreatePlatformSpaceRequest request) {
		RegistrationResponse registrationResponse = registrationService.register(
			new RegistrationRequest(
				request.ownerName(),
				request.ownerEmail(),
				request.ownerPassword(),
				request.spaceName()
			),
			normalizeEnabledModules(request.enabledModules())
		);
		return read(registrationResponse.householdId());
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public List<PlatformAdminSpaceResponse> list() {
		List<Household> households = householdRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();
		return buildResponses(households);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public PlatformAdminSpaceResponse read(Long spaceId) {
		Household household = householdRepository.findByIdAndDeletedAtIsNull(spaceId)
			.orElseThrow(() -> new IllegalArgumentException("Space was not found"));
		return buildResponses(List.of(household)).getFirst();
	}

	@Transactional
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public PlatformAdminSpaceResponse updateModules(Long spaceId, UpdateSpaceModulesRequest request) {
		householdModuleService.updateModules(spaceId, normalizeEnabledModules(request.enabledModules()));
		return read(spaceId);
	}

	private List<PlatformAdminSpaceResponse> buildResponses(List<Household> households) {
		if (households.isEmpty()) {
			return List.of();
		}
		List<Long> householdIds = households.stream().map(Household::getId).toList();
		Map<Long, List<HouseholdMember>> membershipsByHouseholdId = householdMemberRepository
			.findActiveMembershipsByHouseholdIds(householdIds)
			.stream()
			.collect(Collectors.groupingBy(member -> member.getHousehold().getId()));
		Map<Long, List<HouseholdModule>> modulesByHouseholdId = householdModuleService
			.listForHouseholds(householdIds)
			.stream()
			.collect(Collectors.groupingBy(module -> module.getHousehold().getId()));

		return households.stream()
			.map(household -> toResponse(
				household,
				membershipsByHouseholdId.getOrDefault(household.getId(), List.of()),
				modulesByHouseholdId.getOrDefault(household.getId(), List.of())
			))
			.toList();
	}

	private PlatformAdminSpaceResponse toResponse(
		Household household,
		List<HouseholdMember> members,
		List<HouseholdModule> modules
	) {
		HouseholdMember owner = members.stream()
			.filter(member -> member.getRole() == HouseholdMemberRole.OWNER)
			.findFirst()
			.orElse(null);

		return new PlatformAdminSpaceResponse(
			household.getId(),
			household.getName(),
			household.getCreatedAt(),
			household.getUpdatedAt(),
			members.size(),
			owner == null ? null : new PlatformAdminSpaceResponse.Owner(
				owner.getUserId(),
				owner.getUser().getName(),
				owner.getUser().getEmail()
			),
			modules.stream()
				.sorted((left, right) -> left.getModuleKey().compareTo(right.getModuleKey()))
				.map(module -> new PlatformAdminSpaceResponse.Module(
					module.getModuleKey().name(),
					module.isEnabled(),
					module.getModuleKey().isMandatory()
				))
				.toList()
		);
	}

	Set<HouseholdModuleKey> normalizeEnabledModules(List<HouseholdModuleKey> enabledModules) {
		if (enabledModules == null || enabledModules.isEmpty()) {
			return EnumSet.noneOf(HouseholdModuleKey.class);
		}
		return enabledModules.stream().collect(Collectors.toCollection(() -> EnumSet.noneOf(HouseholdModuleKey.class)));
	}
}
