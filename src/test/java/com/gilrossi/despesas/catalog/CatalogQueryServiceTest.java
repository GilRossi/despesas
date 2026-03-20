package com.gilrossi.despesas.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SubcategoryRepository subcategoryRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private CatalogQueryService service;

	@BeforeEach
	void setUp() {
		service = new CatalogQueryService(categoryRepository, subcategoryRepository, currentHouseholdProvider);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_retornar_catalogo_ativo_agrupado_por_categoria() {
		when(categoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(
			new Category(10L, "Casa", true),
			new Category(20L, "Transporte", true)
		));
		when(subcategoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(
			new Subcategory(100L, 10L, "Internet", true),
			new Subcategory(101L, 10L, "Mercado", true),
			new Subcategory(200L, 20L, "Combustivel", true)
		));

		List<CatalogOptionsResponse> options = service.listarOpcoesAtivas();

		assertThat(options).hasSize(2);
		assertThat(options.get(0).id()).isEqualTo(10L);
		assertThat(options.get(0).subcategories()).extracting(CatalogSubcategoryOption::name)
			.containsExactly("Internet", "Mercado");
		assertThat(options.get(1).id()).isEqualTo(20L);
		assertThat(options.get(1).subcategories()).extracting(CatalogSubcategoryOption::name)
			.containsExactly("Combustivel");
	}
}
