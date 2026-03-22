package com.gilrossi.despesas.identity;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class HouseholdMemberService {

	private final AppUserRepository appUserRepository;
	private final HouseholdRepository householdRepository;
	private final HouseholdMemberRepository householdMemberRepository;
	private final PasswordEncoder passwordEncoder;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public HouseholdMemberService(
		AppUserRepository appUserRepository,
		HouseholdRepository householdRepository,
		HouseholdMemberRepository householdMemberRepository,
		PasswordEncoder passwordEncoder,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.appUserRepository = appUserRepository;
		this.householdRepository = householdRepository;
		this.householdMemberRepository = householdMemberRepository;
		this.passwordEncoder = passwordEncoder;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('OWNER')")
	public List<HouseholdMemberResponse> listMembers() {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		return householdMemberRepository.findActiveMembershipsByHouseholdId(householdId).stream()
			.map(HouseholdMemberResponse::from)
			.toList();
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public HouseholdMemberResponse create(CreateHouseholdMemberCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		String name = sanitize(command.name());
		String email = sanitizeEmail(command.email());
		String password = command.password();
		HouseholdMemberRole role = command.role() == null ? HouseholdMemberRole.MEMBER : command.role();
		if (role != HouseholdMemberRole.MEMBER) {
			throw new IllegalArgumentException("Only MEMBER can be created from the household members flow");
		}

		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("name must not be blank");
		}
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("email must not be blank");
		}
		if (!StringUtils.hasText(password)) {
			throw new IllegalArgumentException("password must not be blank");
		}
		if (appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).isPresent()) {
			throw new DuplicateRegistrationException(email);
		}

		Household household = householdRepository.findById(householdId)
			.orElseThrow(() -> new IllegalArgumentException("Current household was not found"));
		AppUser user = appUserRepository.save(new AppUser(name, email, passwordEncoder.encode(password)));
		HouseholdMember member = householdMemberRepository.save(new HouseholdMember(household, user, role));
		return HouseholdMemberResponse.from(member);
	}

	private String sanitize(String value) {
		return value == null ? null : value.trim();
	}

	private String sanitizeEmail(String value) {
		String sanitized = sanitize(value);
		return sanitized == null ? null : sanitized.toLowerCase(Locale.ROOT);
	}
}
