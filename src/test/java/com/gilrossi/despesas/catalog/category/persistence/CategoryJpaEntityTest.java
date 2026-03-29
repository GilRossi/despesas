package com.gilrossi.despesas.catalog.category.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class CategoryJpaEntityTest {

	@Test
	void deve_preencher_e_atualizar_timestamps() {
		CategoryJpaEntity entity = new CategoryJpaEntity();
		entity.setId(1L);
		entity.setHouseholdId(7L);
		entity.setName("Moradia");
		entity.setActive(true);
		entity.prePersist();
		OffsetDateTime createdAt = entity.getCreatedAt();
		OffsetDateTime updatedAt = entity.getUpdatedAt();

		entity.setName("Moradia atualizada");
		entity.preUpdate();
		entity.setDeletedAt(OffsetDateTime.parse("2026-03-29T11:00:00Z"));

		assertThat(entity.getId()).isEqualTo(1L);
		assertThat(entity.getHouseholdId()).isEqualTo(7L);
		assertThat(entity.getName()).isEqualTo("Moradia atualizada");
		assertThat(entity.isActive()).isTrue();
		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
		assertThat(entity.getDeletedAt()).isEqualTo(OffsetDateTime.parse("2026-03-29T11:00:00Z"));
	}
}
