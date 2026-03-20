package com.gilrossi.despesas.catalog;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class CatalogQueryService {

	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public CatalogQueryService(
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public List<CatalogOptionsResponse> listarOpcoesAtivas() {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Map<Long, List<CatalogSubcategoryOption>> subcategoriesByCategoryId = subcategoryRepository.findActiveByHouseholdId(householdId).stream()
			.collect(Collectors.groupingBy(
				Subcategory::getCategoryId,
				Collectors.mapping(subcategory -> new CatalogSubcategoryOption(subcategory.getId(), subcategory.getName()), Collectors.toList())
			));
		return categoryRepository.findActiveByHouseholdId(householdId).stream()
			.map(category -> new CatalogOptionsResponse(
				category.getId(),
				category.getName(),
				subcategoriesByCategoryId.getOrDefault(category.getId(), List.of())
			))
			.toList();
	}
}
