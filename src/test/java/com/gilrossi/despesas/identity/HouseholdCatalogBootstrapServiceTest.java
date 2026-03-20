package com.gilrossi.despesas.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;

@ExtendWith(MockitoExtension.class)
class HouseholdCatalogBootstrapServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SubcategoryRepository subcategoryRepository;

	private HouseholdCatalogBootstrapService service;

	@BeforeEach
	void setUp() {
		service = new HouseholdCatalogBootstrapService(categoryRepository, subcategoryRepository);
	}

	@Test
	void deve_criar_catalogo_padrao_para_household_novo() {
		when(categoryRepository.save(eq(10L), any(Category.class))).thenAnswer(invocation -> {
			Category category = invocation.getArgument(1);
			if ("Geral".equals(category.getName())) {
				category.setId(100L);
			} else {
				category.setId(101L);
			}
			return category;
		});
		when(subcategoryRepository.save(eq(10L), any(Subcategory.class))).thenAnswer(invocation -> invocation.getArgument(1));

		service.bootstrapDefaults(10L);

		ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
		verify(categoryRepository, org.mockito.Mockito.times(2)).save(eq(10L), categoryCaptor.capture());
		assertEquals(List.of("Geral", "Casa"), categoryCaptor.getAllValues().stream().map(Category::getName).toList());

		ArgumentCaptor<Subcategory> subcategoryCaptor = ArgumentCaptor.forClass(Subcategory.class);
		verify(subcategoryRepository, org.mockito.Mockito.times(3)).save(eq(10L), subcategoryCaptor.capture());
		assertEquals(
			List.of("Primeiros lançamentos", "Mercado", "Internet"),
			subcategoryCaptor.getAllValues().stream().map(Subcategory::getName).toList()
		);
	}
}
