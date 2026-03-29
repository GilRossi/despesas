package com.gilrossi.despesas.spacereference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class SpaceReferenceTest {

	@Test
	void deve_expor_grupo_derivado_e_campos_mutaveis() {
		OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-01T10:00:00Z");
		OffsetDateTime updatedAt = OffsetDateTime.parse("2026-03-02T11:00:00Z");
		OffsetDateTime deletedAt = OffsetDateTime.parse("2026-03-03T12:00:00Z");

		SpaceReference reference = new SpaceReference(
			1L,
			10L,
			SpaceReferenceType.APARTAMENTO,
			"Apto 12",
			"apto 12",
			createdAt,
			updatedAt,
			deletedAt
		);

		assertEquals(SpaceReferenceTypeGroup.RESIDENCIAL, reference.getTypeGroup());
		assertEquals(1L, reference.getId());
		assertEquals(10L, reference.getHouseholdId());
		assertEquals("Apto 12", reference.getName());
		assertEquals("apto 12", reference.getNormalizedName());
		assertEquals(createdAt, reference.getCreatedAt());
		assertEquals(updatedAt, reference.getUpdatedAt());
		assertEquals(deletedAt, reference.getDeletedAt());

		reference.setType(SpaceReferenceType.COWORKING);
		reference.setId(2L);
		reference.setHouseholdId(11L);
		reference.setName("Sala comercial");
		reference.setNormalizedName("sala comercial");
		reference.setCreatedAt(updatedAt);
		reference.setUpdatedAt(deletedAt);
		reference.setDeletedAt(createdAt);

		assertEquals(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO, reference.getTypeGroup());
		assertEquals(2L, reference.getId());
		assertEquals(11L, reference.getHouseholdId());
		assertEquals("Sala comercial", reference.getName());
		assertEquals("sala comercial", reference.getNormalizedName());
		assertEquals(updatedAt, reference.getCreatedAt());
		assertEquals(deletedAt, reference.getUpdatedAt());
		assertEquals(createdAt, reference.getDeletedAt());
	}

	@Test
	void deve_tratar_tipo_nulo_e_listar_tipos_por_grupo() {
		SpaceReference reference = new SpaceReference();

		assertNull(reference.getTypeGroup());
		assertTrue(SpaceReferenceType.fromGroup(SpaceReferenceTypeGroup.AVIACAO).contains(SpaceReferenceType.AVIAO));
		assertTrue(SpaceReferenceType.fromGroup(SpaceReferenceTypeGroup.VEICULOS).contains(SpaceReferenceType.CARRO));
	}
}
