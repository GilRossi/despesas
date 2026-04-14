package com.gilrossi.despesas.identity;

import java.util.Set;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RegistrationService {

	private final AppUserRepository appUserRepository;
	private final HouseholdRepository householdRepository;
	private final HouseholdMemberRepository householdMemberRepository;
	private final PasswordEncoder passwordEncoder;
	private final HouseholdCatalogBootstrapService householdCatalogBootstrapService;
	private final HouseholdModuleService householdModuleService;

	public RegistrationService(
		AppUserRepository appUserRepository,
		HouseholdRepository householdRepository,
		HouseholdMemberRepository householdMemberRepository,
		PasswordEncoder passwordEncoder,
		HouseholdCatalogBootstrapService householdCatalogBootstrapService,
		HouseholdModuleService householdModuleService
	) {
		this.appUserRepository = appUserRepository;
		this.householdRepository = householdRepository;
		this.householdMemberRepository = householdMemberRepository;
		this.passwordEncoder = passwordEncoder;
		this.householdCatalogBootstrapService = householdCatalogBootstrapService;
		this.householdModuleService = householdModuleService;
	}

	@Transactional
	public RegistrationResponse register(RegistrationRequest request) {
		return register(request, HouseholdModuleKey.defaultEnabledModules());
	}

	@Transactional
	public RegistrationResponse register(RegistrationRequest request, Set<HouseholdModuleKey> enabledModules) {
		String name = sanitize(request.name());
		String email = sanitizeEmail(request.email());
		String password = request.password();
		String householdName = sanitize(request.householdName());

		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("email must not be blank");
		}
		if (!StringUtils.hasText(password)) {
			throw new IllegalArgumentException("password must not be blank");
		}
		if (!StringUtils.hasText(householdName)) {
			throw new IllegalArgumentException("householdName must not be blank");
		}
		if (appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).isPresent()) {
			throw new DuplicateRegistrationException(email);
		}

		Household household = householdRepository.save(new Household(householdName));
		AppUser user = appUserRepository.save(new AppUser(name, email, passwordEncoder.encode(password)));
		HouseholdMember member = householdMemberRepository.save(new HouseholdMember(household, user, HouseholdMemberRole.OWNER));
		householdCatalogBootstrapService.bootstrapDefaults(household.getId());
		householdModuleService.initializeForHousehold(household.getId(), enabledModules);
		return RegistrationResponse.from(user, member);
	}

	private String sanitize(String value) {
		return value == null ? null : value.trim();
	}

	private String sanitizeEmail(String value) {
		String sanitized = sanitize(value);
		return sanitized == null ? null : sanitized.toLowerCase(Locale.ROOT);
	}
}
