package com.gilrossi.despesas.identity;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;

@Service
public class HouseholdCatalogBootstrapService {

	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;

	public HouseholdCatalogBootstrapService(
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository
	) {
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
	}

	@Transactional
	public void bootstrapDefaults(Long householdId) {
		if (!categoryRepository.findActiveByHouseholdId(householdId).isEmpty()) {
			return;
		}

		Category geral = categoryRepository.save(householdId, new Category(null, "Geral", true));
		subcategoryRepository.save(householdId, new Subcategory(null, geral.getId(), "Primeiros lançamentos", true));

		Category casa = categoryRepository.save(householdId, new Category(null, "Casa", true));
		List.of("Mercado", "Internet").forEach(name ->
			subcategoryRepository.save(householdId, new Subcategory(null, casa.getId(), name, true))
		);
	}
}
