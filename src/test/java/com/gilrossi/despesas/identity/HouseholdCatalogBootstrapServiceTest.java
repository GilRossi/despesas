package com.gilrossi.despesas.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
		when(categoryRepository.findActiveByHouseholdId(10L)).thenReturn(List.of());
		when(subcategoryRepository.findActiveByHouseholdId(10L)).thenReturn(List.of());
		when(categoryRepository.findByNameIgnoreCase(eq(10L), any())).thenReturn(Optional.empty());
		when(categoryRepository.save(eq(10L), any(Category.class))).thenAnswer(invocation -> {
			Category category = invocation.getArgument(1);
			if (category.getId() == null) {
				category.setId(100L + HouseholdCatalogBootstrapService.DEFAULT_CATEGORY_ORDER.indexOf(category.getName()));
			}
			return category;
		});
		when(subcategoryRepository.save(eq(10L), any(Subcategory.class))).thenAnswer(invocation -> invocation.getArgument(1));

		service.bootstrapDefaults(10L);

		ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
		verify(categoryRepository, org.mockito.Mockito.times(10)).save(eq(10L), categoryCaptor.capture());
		assertEquals(HouseholdCatalogBootstrapService.DEFAULT_CATEGORY_ORDER, categoryCaptor.getAllValues().stream().map(Category::getName).toList());

		ArgumentCaptor<Subcategory> subcategoryCaptor = ArgumentCaptor.forClass(Subcategory.class);
		verify(subcategoryRepository, atLeastOnce()).save(eq(10L), subcategoryCaptor.capture());
		assertTrue(subcategoryCaptor.getAllValues().stream().map(Subcategory::getName).toList().contains("Hospedagem"));
		assertTrue(subcategoryCaptor.getAllValues().stream().map(Subcategory::getName).toList().contains("IA"));
	}

	@Test
	void deve_migrar_catalogo_legado_sem_manter_geral_como_categoria_ativa() {
		Map<String, Category> categoriesByName = new LinkedHashMap<>();
		Category geral = new Category(1L, "Geral", true);
		Category casa = new Category(2L, "Casa", true);
		categoriesByName.put(normalize(geral.getName()), geral);
		categoriesByName.put(normalize(casa.getName()), casa);
		List<Subcategory> activeSubcategories = new ArrayList<>(List.of(
			new Subcategory(11L, 1L, "Primeiros lançamentos", true),
			new Subcategory(21L, 2L, "Internet", true),
			new Subcategory(22L, 2L, "Mercado", true)
		));

		when(categoryRepository.findActiveByHouseholdId(10L)).thenAnswer(invocation -> new ArrayList<>(categoriesByName.values().stream().filter(Category::isActive).toList()));
		when(subcategoryRepository.findActiveByHouseholdId(10L)).thenAnswer(invocation -> activeSubcategories.stream().filter(Subcategory::isActive).toList());
		when(categoryRepository.findByNameIgnoreCase(eq(10L), any())).thenAnswer(invocation -> Optional.ofNullable(categoriesByName.get(normalize(invocation.getArgument(1)))));
		when(categoryRepository.save(eq(10L), any(Category.class))).thenAnswer(invocation -> {
			Category category = invocation.getArgument(1);
			if (category.getId() == null) {
				category.setId(100L + categoriesByName.size());
			}
			categoriesByName.put(normalize(category.getName()), category);
			return category;
		});
		when(subcategoryRepository.save(eq(10L), any(Subcategory.class))).thenAnswer(invocation -> {
			Subcategory subcategory = invocation.getArgument(1);
			if (subcategory.getId() == null) {
				subcategory.setId(200L + activeSubcategories.size());
				activeSubcategories.add(subcategory);
				return subcategory;
			}
			for (int index = 0; index < activeSubcategories.size(); index++) {
				if (activeSubcategories.get(index).getId().equals(subcategory.getId())) {
					activeSubcategories.set(index, subcategory);
					return subcategory;
				}
			}
			activeSubcategories.add(subcategory);
			return subcategory;
		});

		service.bootstrapDefaults(10L);

		assertEquals("Moradia", casa.getName());
		assertTrue(!geral.isActive());
		assertTrue(activeSubcategories.stream().anyMatch(subcategory ->
			subcategory.isActive()
				&& "Serviços e Assinaturas".equalsIgnoreCase(categoriesByName.values().stream()
					.filter(category -> category.getId().equals(subcategory.getCategoryId()))
					.findFirst()
					.map(Category::getName)
					.orElse(""))
				&& "Hospedagem".equalsIgnoreCase(subcategory.getName())
		));
		assertTrue(activeSubcategories.stream().anyMatch(subcategory ->
			subcategory.isActive()
				&& "Serviços e Assinaturas".equalsIgnoreCase(categoriesByName.values().stream()
					.filter(category -> category.getId().equals(subcategory.getCategoryId()))
					.findFirst()
					.map(Category::getName)
					.orElse(""))
				&& "IA".equalsIgnoreCase(subcategory.getName())
		));
		assertTrue(activeSubcategories.stream().noneMatch(subcategory ->
			subcategory.isActive()
				&& subcategory.getCategoryId().equals(casa.getId())
				&& "Mercado".equalsIgnoreCase(subcategory.getName())
		));
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
