package com.gilrossi.despesas.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IdentityEntityTest {

	@Test
	void deve_manter_ciclo_de_vida_do_app_user() {
		AppUser user = new AppUser("Ana", "ana@local.invalid", "hash");
		assertThat(user.getPlatformRole()).isEqualTo(PlatformUserRole.STANDARD_USER);

		user.setId(10L);
		user.prePersist();
		Instant createdAt = user.getCreatedAt();
		Instant updatedAt = user.getUpdatedAt();
		assertThat(user.getCredentialsUpdatedAt()).isNotNull();
		assertThat(user.isOnboardingCompleted()).isFalse();

		user.changePasswordHash("hash-2", Instant.parse("2026-03-29T10:00:00Z"));
		user.markOnboardingCompleted(Instant.parse("2026-03-29T10:05:00Z"));
		user.preUpdate();

		assertThat(user.getId()).isEqualTo(10L);
		assertThat(user.getName()).isEqualTo("Ana");
		assertThat(user.getEmail()).isEqualTo("ana@local.invalid");
		assertThat(user.getPasswordHash()).isEqualTo("hash-2");
		assertThat(user.getCredentialsUpdatedAt()).isEqualTo(Instant.parse("2026-03-29T10:00:00Z"));
		assertThat(user.isOnboardingCompleted()).isTrue();
		assertThat(user.getOnboardingCompletedAt()).isEqualTo(Instant.parse("2026-03-29T10:05:00Z"));
		assertThat(user.getCreatedAt()).isEqualTo(createdAt);
		assertThat(user.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
		user.setDeletedAt(Instant.parse("2026-03-29T11:00:00Z"));
		assertThat(user.getDeletedAt()).isEqualTo(Instant.parse("2026-03-29T11:00:00Z"));
	}

	@Test
	void deve_preservar_dados_e_timestamps_do_household() {
		Household household = new Household("Casa da Ana");
		household.setId(7L);
		household.prePersist();
		Instant createdAt = household.getCreatedAt();
		Instant updatedAt = household.getUpdatedAt();

		household.setName("Casa Atualizada");
		household.preUpdate();

		assertThat(household.getId()).isEqualTo(7L);
		assertThat(household.getName()).isEqualTo("Casa Atualizada");
		assertThat(household.getCreatedAt()).isEqualTo(createdAt);
		assertThat(household.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
		household.setDeletedAt(Instant.parse("2026-03-29T12:00:00Z"));
		assertThat(household.getDeletedAt()).isEqualTo(Instant.parse("2026-03-29T12:00:00Z"));
	}

	@Test
	void deve_expor_household_e_user_id_do_membro() {
		Household household = new Household("Casa da Ana");
		household.setId(7L);
		AppUser user = new AppUser("Ana", "ana@local.invalid", "hash");
		user.setId(3L);
		HouseholdMember member = new HouseholdMember(household, user, HouseholdMemberRole.OWNER);
		member.setId(9L);
		member.prePersist();
		Instant updatedAt = member.getUpdatedAt();

		member.setRole(HouseholdMemberRole.MEMBER);
		member.preUpdate();

		assertThat(member.getId()).isEqualTo(9L);
		assertThat(member.getHousehold()).isSameAs(household);
		assertThat(member.getUser()).isSameAs(user);
		assertThat(member.getRole()).isEqualTo(HouseholdMemberRole.MEMBER);
		assertThat(member.getHouseholdId()).isEqualTo(7L);
		assertThat(member.getUserId()).isEqualTo(3L);
		assertThat(member.getCreatedAt()).isNotNull();
		assertThat(member.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
		member.setDeletedAt(Instant.parse("2026-03-29T13:00:00Z"));
		assertThat(member.getDeletedAt()).isEqualTo(Instant.parse("2026-03-29T13:00:00Z"));
	}
}
