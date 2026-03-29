package com.gilrossi.despesas.catalog.subcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.gilrossi.despesas.catalog.subcategory.persistence.SubcategoryJpaEntity;
import com.gilrossi.despesas.catalog.subcategory.persistence.SubcategoryJpaRepository;

@ExtendWith(MockitoExtension.class)
class JpaSubcategoryRepositoryAdapterTest {

	@Mock
	private SubcategoryJpaRepository repository;

	private JpaSubcategoryRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaSubcategoryRepositoryAdapter(repository);
	}

	@Test
	void deve_normalizar_query_mapear_resultados_e_listar_ativas() {
		SubcategoryJpaEntity entity = entity(1L, 10L, 4L, "Internet", true);
		PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));
		when(repository.findAllByFilters(10L, 4L, "internet", true, pageable))
			.thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
		when(repository.findActiveByHouseholdId(10L)).thenReturn(List.of(entity));

		var page = adapter.findAll(10L, 4L, "  Internet  ", true, pageable);

		assertEquals(1, page.getTotalElements());
		assertEquals("Internet", page.getContent().getFirst().getName());
		assertEquals(List.of("Internet"), adapter.findActiveByHouseholdId(10L).stream().map(Subcategory::getName).toList());
	}

	@Test
	void deve_tratar_nome_vazio_sem_consultar_existencia() {
		assertFalse(adapter.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(10L, 4L, "   ", 99L));
		verify(repository, never())
			.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(any(), any(), any(), any());
	}

	@Test
	void deve_salvar_subcategoria_existente_e_desativar_por_categoria() {
		SubcategoryJpaEntity current = entity(1L, 10L, 4L, "Antiga", false);
		SubcategoryJpaEntity saved = entity(1L, 10L, 4L, "Internet", true);
		when(repository.findByIdAndHouseholdIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(current));
		when(repository.saveAndFlush(any(SubcategoryJpaEntity.class))).thenReturn(saved);

		Subcategory result = adapter.save(10L, new Subcategory(1L, 4L, "  Internet  ", true));
		adapter.desativarPorCategoriaId(10L, 4L);

		ArgumentCaptor<SubcategoryJpaEntity> captor = ArgumentCaptor.forClass(SubcategoryJpaEntity.class);
		verify(repository).saveAndFlush(captor.capture());
		assertEquals("Internet", captor.getValue().getName());
		assertEquals(4L, captor.getValue().getCategoryId());
		assertEquals(10L, captor.getValue().getHouseholdId());
		assertTrue(captor.getValue().isActive());
		assertEquals("Internet", result.getName());
		verify(repository).desativarPorCategoriaId(10L, 4L);
		verify(repository).flush();
	}

	@Test
	void deve_buscar_por_id_e_limpar_repositorio() {
		when(repository.findByIdAndHouseholdIdAndDeletedAtIsNull(10L, 2L))
			.thenReturn(Optional.of(entity(2L, 10L, 4L, "Pets", false)));

		Subcategory result = adapter.findById(10L, 2L).orElseThrow();
		adapter.deleteAll();

		assertEquals(2L, result.getId());
		assertFalse(result.isActive());
		verify(repository).deleteAllInBatch();
		verify(repository).flush();
	}

	@Test
	void entidade_jpa_deve_expor_estado_e_ciclo_de_vida() {
		SubcategoryJpaEntity entity = new SubcategoryJpaEntity();
		entity.setId(8L);
		entity.setHouseholdId(10L);
		entity.setCategoryId(3L);
		entity.setName("Mercado");
		entity.setActive(true);
		entity.setCreatedAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
		entity.setUpdatedAt(OffsetDateTime.parse("2026-03-01T11:00:00Z"));
		entity.setDeletedAt(OffsetDateTime.parse("2026-03-01T12:00:00Z"));

		assertEquals(8L, entity.getId());
		assertEquals(10L, entity.getHouseholdId());
		assertEquals(3L, entity.getCategoryId());
		assertEquals("Mercado", entity.getName());
		assertTrue(entity.isActive());
		assertEquals(OffsetDateTime.parse("2026-03-01T10:00:00Z"), entity.getCreatedAt());
		assertEquals(OffsetDateTime.parse("2026-03-01T11:00:00Z"), entity.getUpdatedAt());
		assertEquals(OffsetDateTime.parse("2026-03-01T12:00:00Z"), entity.getDeletedAt());

		invokeLifecycle(entity, "prePersist");
		OffsetDateTime persistedUpdatedAt = entity.getUpdatedAt();
		invokeLifecycle(entity, "preUpdate");
		assertFalse(entity.getUpdatedAt().isBefore(persistedUpdatedAt));
	}

	@Test
	void domain_subcategory_deve_expor_estado_mutavel() {
		Subcategory subcategory = new Subcategory();
		subcategory.setId(3L);
		subcategory.setCategoryId(2L);
		subcategory.setName("Combustível");
		subcategory.setActive(true);

		assertEquals(3L, subcategory.getId());
		assertEquals(2L, subcategory.getCategoryId());
		assertEquals("Combustível", subcategory.getName());
		assertTrue(subcategory.isActive());
	}

	private static SubcategoryJpaEntity entity(
		Long id,
		Long householdId,
		Long categoryId,
		String name,
		boolean active
	) {
		SubcategoryJpaEntity entity = new SubcategoryJpaEntity();
		entity.setId(id);
		entity.setHouseholdId(householdId);
		entity.setCategoryId(categoryId);
		entity.setName(name);
		entity.setActive(active);
		return entity;
	}

	private static void invokeLifecycle(SubcategoryJpaEntity entity, String methodName) {
		try {
			Method method = SubcategoryJpaEntity.class.getDeclaredMethod(methodName);
			method.setAccessible(true);
			method.invoke(entity);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}
}
