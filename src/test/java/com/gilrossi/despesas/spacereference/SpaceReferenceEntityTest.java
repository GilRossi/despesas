package com.gilrossi.despesas.spacereference;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class SpaceReferenceEntityTest {

	@Test
	void deve_expor_dados_e_group_da_referencia() {
		SpaceReference reference = new SpaceReference(
			1L,
			7L,
			SpaceReferenceType.CARRO,
			"Apartamento",
			"apartamento",
			OffsetDateTime.parse("2026-03-29T10:00:00Z"),
			OffsetDateTime.parse("2026-03-29T10:05:00Z"),
			null
		);

		reference.setName("Casa");
		reference.setNormalizedName("casa");
		reference.setType(SpaceReferenceType.CASA);
		reference.setDeletedAt(OffsetDateTime.parse("2026-03-29T11:00:00Z"));

		assertThat(reference.getId()).isEqualTo(1L);
		assertThat(reference.getHouseholdId()).isEqualTo(7L);
		assertThat(reference.getType()).isEqualTo(SpaceReferenceType.CASA);
		assertThat(reference.getTypeGroup()).isEqualTo(SpaceReferenceTypeGroup.RESIDENCIAL);
		assertThat(reference.getName()).isEqualTo("Casa");
		assertThat(reference.getNormalizedName()).isEqualTo("casa");
		assertThat(reference.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-03-29T10:00:00Z"));
		assertThat(reference.getUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-03-29T10:05:00Z"));
		assertThat(reference.getDeletedAt()).isEqualTo(OffsetDateTime.parse("2026-03-29T11:00:00Z"));
	}
}
