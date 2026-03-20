package com.gilrossi.despesas.catalog.category;

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

import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SubcategoryRepository subcategoryRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private CategoryService service;

	@BeforeEach
	void setUp() {
		service = new CategoryService(categoryRepository, subcategoryRepository, currentHouseholdProvider);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_criar_categoria_na_unidade_correta_quando_nome_for_valido() {
		when(categoryRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(7L, "Casa", null)).thenReturn(false);
		when(categoryRepository.save(eq(7L), any(Category.class))).thenAnswer(invocation -> {
			Category categoria = invocation.getArgument(1);
			categoria.setId(1L);
			return categoria;
		});

		Category categoria = service.criar(new CreateCategoryCommand(" Casa ", true));

		assertEquals(1L, categoria.getId());
		assertEquals("Casa", categoria.getName());
		assertEquals(true, categoria.isActive());
	}

	@Test
	void deve_rejeitar_categoria_duplicada_por_nome_ativo() {
		when(categoryRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(7L, "Casa", null)).thenReturn(true);

		assertThrows(DuplicateCategoryException.class, () -> service.criar(new CreateCategoryCommand("Casa", true)));
	}

	@Test
	void deve_atualizar_categoria_existente() {
		Category categoria = new Category(10L, "Casa", true);
		when(categoryRepository.findById(7L, 10L)).thenReturn(Optional.of(categoria));
		when(categoryRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(7L, "Lar", 10L)).thenReturn(false);
		when(categoryRepository.save(eq(7L), any(Category.class))).thenReturn(categoria);

		Category atualizada = service.atualizar(10L, new UpdateCategoryCommand("Lar", false));

		assertEquals("Lar", atualizada.getName());
		assertFalse(atualizada.isActive());
	}

	@Test
	void deve_desativar_categoria_e_suas_subcategorias() {
		Category categoria = new Category(10L, "Casa", true);
		when(categoryRepository.findById(7L, 10L)).thenReturn(Optional.of(categoria));
		when(categoryRepository.save(eq(7L), any(Category.class))).thenReturn(categoria);

		service.desativar(10L);

		verify(subcategoryRepository).desativarPorCategoriaId(7L, 10L);
		assertFalse(categoria.isActive());
	}

	@Test
	void deve_listar_categorias_ordenadas() {
		when(categoryRepository.findAll(eq(7L), isNull(), isNull(), any(PageRequest.class))).thenReturn(
			new PageImpl<>(List.of(new Category(2L, "Carro", true), new Category(1L, "Casa", true)))
		);

		assertEquals("Carro", service.listar(null, null, 0, 10).getContent().get(0).getName());
	}
}
