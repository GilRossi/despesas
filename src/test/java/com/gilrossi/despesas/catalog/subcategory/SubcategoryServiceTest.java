package com.gilrossi.despesas.catalog.subcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.catalog.subcategory.DuplicateSubcategoryException;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class SubcategoryServiceTest {

	@Mock
	private SubcategoryRepository subcategoryRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private SubcategoryService service;

	@BeforeEach
	void setUp() {
		service = new SubcategoryService(subcategoryRepository, categoryRepository, expenseRepository, currentHouseholdProvider);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_criar_subcategoria_na_unidade_correta_quando_categoria_existir() {
		Category categoria = new Category(1L, "Casa", true);
		when(categoryRepository.findById(7L, 1L)).thenReturn(Optional.of(categoria));
		when(subcategoryRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(7L, 1L, "Mercado", null)).thenReturn(false);
		when(subcategoryRepository.save(eq(7L), any(Subcategory.class))).thenAnswer(invocation -> {
			Subcategory subcategoria = invocation.getArgument(1);
			subcategoria.setId(10L);
			return subcategoria;
		});

		Subcategory subcategoria = service.criar(new CreateSubcategoryCommand(1L, " Mercado ", true));

		assertEquals(10L, subcategoria.getId());
		assertEquals("Mercado", subcategoria.getName());
		assertEquals(1L, subcategoria.getCategoryId());
	}

	@Test
	void deve_rejeitar_subcategoria_duplicada_na_mesma_categoria() {
		when(categoryRepository.findById(7L, 1L)).thenReturn(Optional.of(new Category(1L, "Casa", true)));
		when(subcategoryRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(7L, 1L, "Mercado", null)).thenReturn(true);

		assertThrows(DuplicateSubcategoryException.class, () -> service.criar(new CreateSubcategoryCommand(1L, "Mercado", true)));
	}

	@Test
	void deve_desativar_subcategoria_existente() {
		Subcategory subcategoria = new Subcategory(10L, 1L, "Mercado", true);
		when(subcategoryRepository.findById(7L, 10L)).thenReturn(Optional.of(subcategoria));
		when(subcategoryRepository.save(eq(7L), any(Subcategory.class))).thenReturn(subcategoria);

		service.desativar(10L);

		assertFalse(subcategoria.isActive());
		verify(subcategoryRepository).save(7L, subcategoria);
	}

	@Test
	void deve_listar_subcategorias_ordenadas() {
		when(subcategoryRepository.findAll(eq(7L), isNull(), isNull(), isNull(), any(PageRequest.class))).thenReturn(
			new PageImpl<>(List.of(new Subcategory(2L, 1L, "Carro", true), new Subcategory(1L, 1L, "Casa", true)))
		);

		assertEquals("Carro", service.listar(null, null, null, 0, 10).getContent().get(0).getName());
	}

	@Test
	void deve_rejeitar_mudanca_de_categoria_quando_subcategoria_ja_tiver_despesas() {
		Subcategory subcategoria = new Subcategory(10L, 1L, "Mercado", true);
		when(subcategoryRepository.findById(7L, 10L)).thenReturn(Optional.of(subcategoria));
		when(categoryRepository.findById(7L, 2L)).thenReturn(Optional.of(new Category(2L, "Lazer", true)));
		when(subcategoryRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(7L, 2L, "Mercado", 10L)).thenReturn(false);
		when(expenseRepository.existsByHouseholdIdAndSubcategoryId(7L, 10L)).thenReturn(true);

		IllegalStateException error = assertThrows(IllegalStateException.class, () ->
			service.atualizar(10L, new UpdateSubcategoryCommand(2L, "Mercado", true))
		);

		assertEquals("Subcategory with linked expenses cannot change category", error.getMessage());
	}
}
