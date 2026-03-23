package com.gilrossi.despesas.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;

@Service
public class HouseholdUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;
	private final HouseholdMemberRepository householdMemberRepository;

	public HouseholdUserDetailsService(AppUserRepository appUserRepository, HouseholdMemberRepository householdMemberRepository) {
		this.appUserRepository = appUserRepository;
		this.householdMemberRepository = householdMemberRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AuthenticatedHouseholdUser loadUserByUsername(String username) {
		String normalizedUsername = username == null ? null : username.trim().toLowerCase(Locale.ROOT);
		AppUser user = appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedUsername)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return toAuthenticatedUser(user, username);
	}

	@Transactional(readOnly = true)
	public AuthenticatedHouseholdUser loadUserById(Long userId) {
		AppUser user = appUserRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new UsernameNotFoundException("User not found for id: " + userId));
		return toAuthenticatedUser(user, String.valueOf(userId));
	}

	private AuthenticatedHouseholdUser toAuthenticatedUser(AppUser user, String lookupReference) {
		if (user.getPlatformRole() == PlatformUserRole.PLATFORM_ADMIN) {
			return AuthenticatedHouseholdUser.platformAdmin(user);
		}
		HouseholdMember member = householdMemberRepository.findFirstActiveMembershipByUserId(user.getId())
			.orElseThrow(() -> new UsernameNotFoundException("Household membership not found for user: " + lookupReference));
		return AuthenticatedHouseholdUser.from(user, member);
	}
}
