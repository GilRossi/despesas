package com.gilrossi.despesas.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IdentityDomainModelTest {

	@Test
	void app_user_deve_atualizar_credenciais_onboarding_e_ciclo_de_vida() {
		AppUser user = new AppUser("Gil", "gil@example.com", "hash-inicial");

		assertEquals(PlatformUserRole.STANDARD_USER, user.getPlatformRole());
		assertFalse(user.isOnboardingCompleted());
		assertNull(user.getOnboardingCompletedAt());

		user.setId(7L);
		user.setName("Gil Rossi");
		user.setEmail("gil.rossi@example.com");
		user.setPasswordHash("hash-atual");
		user.setPlatformRole(PlatformUserRole.PLATFORM_ADMIN);
		user.setDeletedAt(Instant.parse("2026-03-28T10:00:00Z"));
		user.prePersist();
		Instant persistedUpdatedAt = user.getUpdatedAt();
		Instant credentialsUpdatedAt = user.getCredentialsUpdatedAt();

		assertEquals(7L, user.getId());
		assertEquals("Gil Rossi", user.getName());
		assertEquals("gil.rossi@example.com", user.getEmail());
		assertEquals("hash-atual", user.getPasswordHash());
		assertEquals(PlatformUserRole.PLATFORM_ADMIN, user.getPlatformRole());
		assertEquals(Instant.parse("2026-03-28T10:00:00Z"), user.getDeletedAt());
		assertNotNull(user.getCreatedAt());
		assertNotNull(persistedUpdatedAt);
		assertNotNull(credentialsUpdatedAt);

		Instant changedAt = Instant.parse("2026-03-29T11:15:00Z");
		user.changePasswordHash("hash-final", changedAt);
		assertEquals("hash-final", user.getPasswordHash());
		assertEquals(changedAt, user.getCredentialsUpdatedAt());

		Instant onboardingAt = Instant.parse("2026-03-29T12:00:00Z");
		user.markOnboardingCompleted(onboardingAt);
		user.markOnboardingCompleted(Instant.parse("2026-03-29T13:00:00Z"));

		assertTrue(user.isOnboardingCompleted());
		assertEquals(onboardingAt, user.getOnboardingCompletedAt());

		user.preUpdate();
		assertFalse(user.getUpdatedAt().isBefore(persistedUpdatedAt));
	}

	@Test
	void household_deve_expor_estado_e_ciclo_de_vida() {
		Household household = new Household("Casa da Ana");

		household.setId(11L);
		household.setName("Casa principal");
		household.setDeletedAt(Instant.parse("2026-03-28T08:00:00Z"));
		household.prePersist();
		Instant persistedUpdatedAt = household.getUpdatedAt();

		assertEquals(11L, household.getId());
		assertEquals("Casa principal", household.getName());
		assertEquals(Instant.parse("2026-03-28T08:00:00Z"), household.getDeletedAt());
		assertNotNull(household.getCreatedAt());
		assertNotNull(persistedUpdatedAt);

		household.preUpdate();
		assertFalse(household.getUpdatedAt().isBefore(persistedUpdatedAt));
	}

	@Test
	void household_member_deve_expor_ids_derivados_e_ciclo_de_vida() {
		Household household = new Household("Casa da Ana");
		household.setId(21L);
		AppUser user = new AppUser("Bia", "bia@example.com", "hash");
		user.setId(22L);

		HouseholdMember member = new HouseholdMember(
			household,
			user,
			HouseholdMemberRole.OWNER
		);

		member.setId(23L);
		member.setRole(HouseholdMemberRole.MEMBER);
		member.setDeletedAt(Instant.parse("2026-03-27T09:30:00Z"));
		member.prePersist();
		Instant persistedUpdatedAt = member.getUpdatedAt();

		assertEquals(23L, member.getId());
		assertEquals(household, member.getHousehold());
		assertEquals(user, member.getUser());
		assertEquals(HouseholdMemberRole.MEMBER, member.getRole());
		assertEquals(21L, member.getHouseholdId());
		assertEquals(22L, member.getUserId());
		assertEquals(Instant.parse("2026-03-27T09:30:00Z"), member.getDeletedAt());
		assertNotNull(member.getCreatedAt());
		assertNotNull(persistedUpdatedAt);

		member.setHousehold(null);
		member.setUser(null);
		assertNull(member.getHouseholdId());
		assertNull(member.getUserId());

		member.preUpdate();
		assertFalse(member.getUpdatedAt().isBefore(persistedUpdatedAt));
	}
}
