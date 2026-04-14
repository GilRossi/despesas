package com.gilrossi.despesas.api.v1.admin;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.identity.HouseholdModuleKey;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

@Service
public class PlatformAdminHouseholdProvisioningService {

	private final RegistrationService registrationService;

	public PlatformAdminHouseholdProvisioningService(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@Transactional
	@PreAuthorize("hasRole('PLATFORM_ADMIN')")
	public HouseholdOwnerProvisioningResponse createHouseholdWithOwner(CreateHouseholdOwnerRequest request) {
		RegistrationResponse response = registrationService.register(
			new RegistrationRequest(
				request.ownerName(),
				request.ownerEmail(),
				request.ownerPassword(),
				request.householdName()
			),
			normalizeEnabledModules(request.enabledModules())
		);
		return new HouseholdOwnerProvisioningResponse(
			response.householdId(),
			request.householdName().trim(),
			response.userId(),
			response.email(),
			response.role()
		);
	}

	private Set<HouseholdModuleKey> normalizeEnabledModules(java.util.List<HouseholdModuleKey> enabledModules) {
		if (enabledModules == null || enabledModules.isEmpty()) {
			return EnumSet.noneOf(HouseholdModuleKey.class);
		}
		return enabledModules.stream()
			.collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(HouseholdModuleKey.class)));
	}
}
