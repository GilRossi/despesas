package com.gilrossi.despesas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.Household;
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class HouseholdUserDetailsServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private HouseholdMemberRepository householdMemberRepository;

	@Test
	void deve_carregar_principal_com_household_e_role() {
		AppUser user = new AppUser("Ana", "ana@local.invalid", "{bcrypt}hash");
		user.setId(7L);

		Household household = new Household("Casa da Ana");
		household.setId(11L);

		HouseholdMember member = new HouseholdMember(household, user, HouseholdMemberRole.OWNER);
		member.setId(13L);

		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@local.invalid"))
			.thenReturn(Optional.of(user));
		when(householdMemberRepository.findFirstActiveMembershipByUserId(7L))
			.thenReturn(Optional.of(member));

		HouseholdUserDetailsService service = new HouseholdUserDetailsService(appUserRepository, householdMemberRepository);
		UserDetails details = service.loadUserByUsername(" ANA@LOCAL.INVALID ");

		assertThat(details).isInstanceOf(AuthenticatedHouseholdUser.class);
		AuthenticatedHouseholdUser principal = (AuthenticatedHouseholdUser) details;
		assertThat(principal.getUserId()).isEqualTo(7L);
		assertThat(principal.getHouseholdId()).isEqualTo(11L);
		assertThat(principal.getRole()).isEqualTo("OWNER");
		assertThat(principal.getAuthorities()).extracting("authority").contains("ROLE_OWNER");
	}
}
