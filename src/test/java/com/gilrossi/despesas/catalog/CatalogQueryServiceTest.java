package com.gilrossi.despesas.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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
import com.gilrossi.despesas.identity.HouseholdCatalogBootstrapService;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SubcategoryRepository subcategoryRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	@Mock
	private HouseholdCatalogBootstrapService householdCatalogBootstrapService;

	private CatalogQueryService service;

	@BeforeEach
	void setUp() {
		service = new CatalogQueryService(categoryRepository, subcategoryRepository, currentHouseholdProvider, householdCatalogBootstrapService);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_retornar_catalogo_ativo_agrupado_por_categoria() {
		when(categoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(
			new Category(20L, "Transporte", true),
			new Category(10L, "Moradia", true),
			new Category(30L, "Categoria customizada", true)
		));
		when(subcategoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(
			new Subcategory(100L, 10L, "Internet", true),
			new Subcategory(101L, 10L, "Aluguel", true),
			new Subcategory(200L, 20L, "Combustível", true),
			new Subcategory(300L, 30L, "Zeta", true),
			new Subcategory(301L, 30L, "Alpha", true)
		));

		List<CatalogOptionsResponse> options = service.listarOpcoesAtivas();

		verify(householdCatalogBootstrapService).bootstrapDefaults(7L);
		assertThat(options).hasSize(3);
		assertThat(options.get(0).id()).isEqualTo(10L);
		assertThat(options.get(0).subcategories()).extracting(CatalogSubcategoryOption::name)
			.containsExactly("Aluguel", "Internet");
		assertThat(options.get(1).id()).isEqualTo(20L);
		assertThat(options.get(1).subcategories()).extracting(CatalogSubcategoryOption::name)
			.containsExactly("Combustível");
		assertThat(options.get(2).name()).isEqualTo("Categoria customizada");
		assertThat(options.get(2).subcategories()).extracting(CatalogSubcategoryOption::name)
			.containsExactly("Alpha", "Zeta");
	}
}
