package com.gilrossi.despesas.spacereference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.spacereference.persistence.SpaceReferenceJpaEntity;
import com.gilrossi.despesas.spacereference.persistence.SpaceReferenceJpaRepository;

@ExtendWith(MockitoExtension.class)
class JpaSpaceReferenceRepositoryAdapterTest {

	@Mock
	private SpaceReferenceJpaRepository repository;

	private JpaSpaceReferenceRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaSpaceReferenceRepositoryAdapter(repository);
	}

	@Test
	void deve_listar_sem_like_quando_q_for_nulo() {
		when(repository.findAllByDeletedAtIsNullAndHouseholdIdOrderByTypeAscNameAscIdAsc(7L))
			.thenReturn(List.of(entity(11L, 7L, SpaceReferenceType.CASA, "Apartamento Central", "apartamento central")));

		List<SpaceReference> result = adapter.findAll(7L, null, null, null);

		assertEquals(1, result.size());
		assertEquals("Apartamento Central", result.getFirst().getName());
		verify(repository).findAllByDeletedAtIsNullAndHouseholdIdOrderByTypeAscNameAscIdAsc(7L);
		verify(repository, never()).findAllByFilters(eq(7L), any(), any());
		verify(repository, never()).findAllByTypeInAndFilters(eq(7L), any(), any());
	}

	@Test
	void deve_listar_sem_like_quando_q_for_branco() {
		when(repository.findAllByDeletedAtIsNullAndHouseholdIdAndTypeOrderByTypeAscNameAscIdAsc(7L, SpaceReferenceType.CASA))
			.thenReturn(List.of(entity(11L, 7L, SpaceReferenceType.CASA, "Apartamento Central", "apartamento central")));

		List<SpaceReference> result = adapter.findAll(7L, null, SpaceReferenceType.CASA, "   ");

		assertEquals(1, result.size());
		verify(repository).findAllByDeletedAtIsNullAndHouseholdIdAndTypeOrderByTypeAscNameAscIdAsc(7L, SpaceReferenceType.CASA);
		verify(repository, never()).findAllByFilters(eq(7L), any(), any());
	}

	@Test
	void deve_manter_busca_textual_quando_q_estiver_preenchido() {
		when(repository.findAllByTypeInAndFilters(7L, SpaceReferenceType.fromGroup(SpaceReferenceTypeGroup.RESIDENCIAL), "apartamento"))
			.thenReturn(List.of(entity(11L, 7L, SpaceReferenceType.CASA, "Apartamento Central", "apartamento central")));

		List<SpaceReference> result = adapter.findAll(7L, SpaceReferenceTypeGroup.RESIDENCIAL, null, "Apartamento");

		assertEquals(1, result.size());
		assertTrue(result.getFirst().getNormalizedName().contains("apartamento"));
		verify(repository).findAllByTypeInAndFilters(
			7L,
			SpaceReferenceType.fromGroup(SpaceReferenceTypeGroup.RESIDENCIAL),
			"apartamento"
		);
		verify(repository, never()).findAllByDeletedAtIsNullAndHouseholdIdAndTypeInOrderByTypeAscNameAscIdAsc(any(), any());
	}

	private SpaceReferenceJpaEntity entity(
		Long id,
		Long householdId,
		SpaceReferenceType type,
		String name,
		String normalizedName
	) {
		SpaceReferenceJpaEntity entity = new SpaceReferenceJpaEntity();
		entity.setId(id);
		entity.setHouseholdId(householdId);
		entity.setType(type);
		entity.setName(name);
		entity.setNormalizedName(normalizedName);
		return entity;
	}
}
