package com.gilrossi.despesas.catalog.subcategory.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class SubcategoryJpaEntityTest {

	@Test
	void deve_preencher_e_atualizar_timestamps() {
		SubcategoryJpaEntity entity = new SubcategoryJpaEntity();
		entity.setId(1L);
		entity.setHouseholdId(7L);
		entity.setCategoryId(10L);
		entity.setName("Internet");
		entity.setActive(true);
		entity.prePersist();
		OffsetDateTime createdAt = entity.getCreatedAt();
		OffsetDateTime updatedAt = entity.getUpdatedAt();

		entity.setName("Internet atualizada");
		entity.preUpdate();
		entity.setDeletedAt(OffsetDateTime.parse("2026-03-29T11:00:00Z"));

		assertThat(entity.getId()).isEqualTo(1L);
		assertThat(entity.getHouseholdId()).isEqualTo(7L);
		assertThat(entity.getCategoryId()).isEqualTo(10L);
		assertThat(entity.getName()).isEqualTo("Internet atualizada");
		assertThat(entity.isActive()).isTrue();
		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
		assertThat(entity.getDeletedAt()).isEqualTo(OffsetDateTime.parse("2026-03-29T11:00:00Z"));
	}
}
