package com.gilrossi.despesas.catalog.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.gilrossi.despesas.catalog.category.persistence.CategoryJpaEntity;
import com.gilrossi.despesas.catalog.category.persistence.CategoryJpaRepository;

@ExtendWith(MockitoExtension.class)
class JpaCategoryRepositoryAdapterTest {

	@Mock
	private CategoryJpaRepository repository;

	private JpaCategoryRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new JpaCategoryRepositoryAdapter(repository);
	}

	@Test
	void deve_normalizar_query_e_mapear_resultados() {
		CategoryJpaEntity entity = entity(1L, 10L, "Casa", true);
		PageRequest pageable = PageRequest.of(0, 10, Sort.by("name"));
		when(repository.findAllByFilters(10L, "casa", true, pageable))
			.thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

		var page = adapter.findAll(10L, "  Casa  ", true, pageable);

		assertEquals(1, page.getTotalElements());
		assertEquals("Casa", page.getContent().getFirst().getName());
		assertTrue(page.getContent().getFirst().isActive());
	}

	@Test
	void deve_tratar_nome_vazio_sem_consultar_repositorio() {
		assertEquals(Optional.empty(), adapter.findByNameIgnoreCase(10L, "   "));
		assertFalse(adapter.existsByNameIgnoreCaseAndIdNotAndActiveTrue(10L, "   ", 99L));

		verify(repository, never()).findByNameIgnoreCaseAndHouseholdIdAndDeletedAtIsNull(any(), any());
		verify(repository, never()).existsByNameIgnoreCaseAndIdNotAndHouseholdIdAndActiveTrue(any(), any(), any());
	}

	@Test
	void deve_salvar_categoria_existente_e_listar_ativas() {
		CategoryJpaEntity current = entity(1L, 10L, "Antiga", false);
		CategoryJpaEntity saved = entity(1L, 10L, "Moradia", true);
		when(repository.findByIdAndHouseholdIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(current));
		when(repository.saveAndFlush(any(CategoryJpaEntity.class))).thenReturn(saved);
		when(repository.findActiveByHouseholdId(10L)).thenReturn(List.of(saved));

		Category result = adapter.save(10L, new Category(1L, "  Moradia  ", true));

		ArgumentCaptor<CategoryJpaEntity> captor = ArgumentCaptor.forClass(CategoryJpaEntity.class);
		verify(repository).saveAndFlush(captor.capture());
		assertEquals("Moradia", captor.getValue().getName());
		assertEquals(10L, captor.getValue().getHouseholdId());
		assertTrue(captor.getValue().isActive());
		assertEquals("Moradia", result.getName());
		assertEquals(List.of("Moradia"), adapter.findActiveByHouseholdId(10L).stream().map(Category::getName).toList());
	}

	@Test
	void deve_buscar_por_id_e_limpar_repositorio() {
		when(repository.findByIdAndHouseholdIdAndDeletedAtIsNull(10L, 2L))
			.thenReturn(Optional.of(entity(2L, 10L, "Pets", false)));

		Category result = adapter.findById(10L, 2L).orElseThrow();
		adapter.deleteAll();

		assertEquals(2L, result.getId());
		assertFalse(result.isActive());
		verify(repository).deleteAllInBatch();
		verify(repository).flush();
	}

	@Test
	void entidade_jpa_deve_expor_estado_e_ciclo_de_vida() {
		CategoryJpaEntity entity = new CategoryJpaEntity();
		entity.setId(8L);
		entity.setHouseholdId(10L);
		entity.setName("Lazer");
		entity.setActive(true);
		entity.setCreatedAt(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
		entity.setUpdatedAt(OffsetDateTime.parse("2026-03-01T11:00:00Z"));
		entity.setDeletedAt(OffsetDateTime.parse("2026-03-01T12:00:00Z"));

		assertEquals(8L, entity.getId());
		assertEquals(10L, entity.getHouseholdId());
		assertEquals("Lazer", entity.getName());
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
	void domain_category_deve_expor_estado_mutavel() {
		Category category = new Category();
		category.setId(3L);
		category.setName("Transporte");
		category.setActive(true);

		assertEquals(3L, category.getId());
		assertEquals("Transporte", category.getName());
		assertTrue(category.isActive());
	}

	private static CategoryJpaEntity entity(Long id, Long householdId, String name, boolean active) {
		CategoryJpaEntity entity = new CategoryJpaEntity();
		entity.setId(id);
		entity.setHouseholdId(householdId);
		entity.setName(name);
		entity.setActive(active);
		return entity;
	}

	private static void invokeLifecycle(CategoryJpaEntity entity, String methodName) {
		try {
			Method method = CategoryJpaEntity.class.getDeclaredMethod(methodName);
			method.setAccessible(true);
			method.invoke(entity);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}
}
