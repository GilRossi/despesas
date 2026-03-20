package com.gilrossi.despesas.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private HouseholdRepository householdRepository;

	@Mock
	private HouseholdMemberRepository householdMemberRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private HouseholdCatalogBootstrapService householdCatalogBootstrapService;

	private RegistrationService service;

	@BeforeEach
	void setUp() {
		service = new RegistrationService(
			appUserRepository,
			householdRepository,
			householdMemberRepository,
			passwordEncoder,
			householdCatalogBootstrapService
		);
	}

	@Test
	void deve_criar_household_usuario_e_membership_owner() {
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@local.invalid")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("senha123")).thenReturn("{bcrypt}senha-criptografada");
		when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
			Household household = invocation.getArgument(0);
			household.setId(10L);
			return household;
		});
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

		RegistrationResponse response = service.register(new RegistrationRequest(
			"Ana",
			"ana@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		assertNotNull(response);
		assertEquals(10L, response.householdId());
		assertEquals(20L, response.userId());
		assertEquals("ana@local.invalid", response.email());
		assertEquals("OWNER", response.role());
		verify(householdCatalogBootstrapService).bootstrapDefaults(10L);
	}

	@Test
	void deve_rejeitar_registro_quando_email_ja_existir() {
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@local.invalid"))
			.thenReturn(Optional.of(new AppUser()));

		assertThrows(DuplicateRegistrationException.class, () ->
			service.register(new RegistrationRequest("Ana", "ana@local.invalid", "senha123", "Casa"))
		);
	}

	@Test
	void deve_normalizar_campos_basicos_no_registro() {
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@local.invalid")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("senha123")).thenReturn("{bcrypt}senha-criptografada");
		when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
			Household household = invocation.getArgument(0);
			household.setId(10L);
			return household;
		});
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

		RegistrationResponse response = service.register(new RegistrationRequest(
			"  Ana  ",
			"  ANA@LOCAL.INVALID  ",
			"senha123",
			"  Casa da Ana  "
		));

		assertEquals("ana@local.invalid", response.email());
	}

	@Test
	void deve_bootstrapar_catalogo_inicial_para_household_novo() {
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@local.invalid")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("senha123")).thenReturn("{bcrypt}senha-criptografada");
		when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> {
			Household household = invocation.getArgument(0);
			household.setId(44L);
			return household;
		});
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

		service.register(new RegistrationRequest("Ana", "ana@local.invalid", "senha123", "Casa"));

		verify(householdCatalogBootstrapService).bootstrapDefaults(eq(44L));
	}
}
