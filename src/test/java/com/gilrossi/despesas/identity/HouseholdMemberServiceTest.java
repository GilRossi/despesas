package com.gilrossi.despesas.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class HouseholdMemberServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private HouseholdRepository householdRepository;

	@Mock
	private HouseholdMemberRepository householdMemberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private HouseholdMemberService service;

	@BeforeEach
	void setUp() {
		service = new HouseholdMemberService(
			appUserRepository,
			householdRepository,
			householdMemberRepository,
			passwordEncoder,
			currentHouseholdProvider
		);
	}

	@Test
	void deve_criar_membro_no_household_atual_com_role_member_por_padrao() {
		Household household = new Household("Casa da Ana");
		household.setId(10L);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(10L);
		when(householdRepository.findById(10L)).thenReturn(Optional.of(household));
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("bia@local.invalid")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("senha123")).thenReturn("{bcrypt}senha-criptografada");
		when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
			AppUser user = invocation.getArgument(0);
			user.setId(20L);
			return user;
		});
		when(householdMemberRepository.save(any(HouseholdMember.class))).thenAnswer(invocation -> {
			HouseholdMember member = invocation.getArgument(0);
			member.setId(30L);
			return member;
		});

		HouseholdMemberResponse response = service.create(new CreateHouseholdMemberCommand(
			"  Bia  ",
			"  BIA@LOCAL.INVALID  ",
			"senha123",
			null
		));

		assertEquals(30L, response.id());
		assertEquals(20L, response.userId());
		assertEquals(10L, response.householdId());
		assertEquals("bia@local.invalid", response.email());
		assertEquals(HouseholdMemberRole.MEMBER, response.role());
	}

	@Test
	void deve_rejeitar_membro_quando_email_ja_estiver_em_uso() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(10L);
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("bia@local.invalid"))
			.thenReturn(Optional.of(new AppUser()));

		assertThrows(DuplicateRegistrationException.class, () ->
			service.create(new CreateHouseholdMemberCommand(
				"Bia",
				"bia@local.invalid",
				"senha123",
				HouseholdMemberRole.MEMBER
			))
		);
	}

	@Test
	void deve_rejeitar_quando_owner_tentar_criar_outro_owner() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(10L);

		assertThrows(IllegalArgumentException.class, () ->
			service.create(new CreateHouseholdMemberCommand(
				"Bia",
				"bia-owner@local.invalid",
				"senha123",
				HouseholdMemberRole.OWNER
			))
		);
	}
}
