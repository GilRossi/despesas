package com.gilrossi.despesas.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.identity.HouseholdCatalogBootstrapService;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class CatalogQueryService {

	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;
	private final HouseholdCatalogBootstrapService householdCatalogBootstrapService;

	public CatalogQueryService(
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		CurrentHouseholdProvider currentHouseholdProvider,
		HouseholdCatalogBootstrapService householdCatalogBootstrapService
	) {
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
		this.householdCatalogBootstrapService = householdCatalogBootstrapService;
	}

	@Transactional
	public List<CatalogOptionsResponse> listarOpcoesAtivas() {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		householdCatalogBootstrapService.bootstrapDefaults(householdId);
		Map<Long, List<CatalogSubcategoryOption>> subcategoriesByCategoryId = subcategoryRepository.findActiveByHouseholdId(householdId).stream()
			.collect(Collectors.groupingBy(
				Subcategory::getCategoryId,
				Collectors.collectingAndThen(
					Collectors.mapping(subcategory -> new CatalogSubcategoryOption(subcategory.getId(), subcategory.getName()), Collectors.toList()),
					subcategories -> subcategories.stream()
						.sorted(Comparator.comparing(option -> normalize(option.name())))
						.toList()
				)
			));
		return categoryRepository.findActiveByHouseholdId(householdId).stream()
			.sorted(Comparator
				.<com.gilrossi.despesas.catalog.category.Category>comparingInt(category -> categorySortOrder(category.getName()))
				.thenComparing(category -> normalize(category.getName())))
			.map(category -> new CatalogOptionsResponse(
				category.getId(),
				category.getName(),
				subcategoriesByCategoryId.getOrDefault(category.getId(), List.of())
			))
			.toList();
	}

	private int categorySortOrder(String name) {
		int index = HouseholdCatalogBootstrapService.DEFAULT_CATEGORY_ORDER.indexOf(name);
		return index >= 0 ? index : Integer.MAX_VALUE;
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
