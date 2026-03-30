package com.gilrossi.despesas.identity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;

@Service
public class HouseholdCatalogBootstrapService {

	public static final List<String> DEFAULT_CATEGORY_ORDER = List.of(
		"Moradia",
		"Alimentação",
		"Transporte",
		"Trabalho",
		"Saúde",
		"Pessoal",
		"Lazer",
		"Serviços e Assinaturas",
		"Financeiro / Obrigações",
		"Outros"
	);

	private static final String LEGACY_GENERAL_CATEGORY = "Geral";
	private static final String LEGACY_GENERAL_SUBCATEGORY = "Primeiros lançamentos";
	private static final String LEGACY_HOUSING_CATEGORY = "Casa";
	private static final String LEGACY_GROCERY_SUBCATEGORY = "Mercado";

	private static final Map<String, List<String>> DEFAULT_TAXONOMY = defaultTaxonomy();

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
		Map<String, Category> categoriesByName = categoryRepository.findActiveByHouseholdId(householdId).stream()
			.collect(Collectors.toMap(
				category -> normalize(category.getName()),
				category -> category,
				(existing, ignored) -> existing,
				LinkedHashMap::new
			));
		List<Subcategory> activeSubcategories = subcategoryRepository.findActiveByHouseholdId(householdId);

		Category moradia = migrateLegacyHousingCategory(householdId, categoriesByName, activeSubcategories);
		deactivateLegacyGeneralCategory(householdId, categoriesByName, activeSubcategories);
		ensureMinimumCategories(householdId, categoriesByName);
		if (moradia == null) {
			moradia = categoriesByName.get(normalize("Moradia"));
		}
		if (moradia != null) {
			deactivateLegacyGroceryUnderHousing(householdId, moradia.getId(), activeSubcategories);
		}
		ensureMinimumSubcategories(householdId, categoriesByName);
	}

	private Category migrateLegacyHousingCategory(
		Long householdId,
		Map<String, Category> categoriesByName,
		List<Subcategory> activeSubcategories
	) {
		Category legacyHousing = categoriesByName.get(normalize(LEGACY_HOUSING_CATEGORY));
		if (legacyHousing == null) {
			return categoriesByName.get(normalize("Moradia"));
		}

		Category currentHousing = categoriesByName.get(normalize("Moradia"));
		if (currentHousing == null) {
			legacyHousing.setName("Moradia");
			Category saved = categoryRepository.save(householdId, legacyHousing);
			categoriesByName.remove(normalize(LEGACY_HOUSING_CATEGORY));
			categoriesByName.put(normalize(saved.getName()), saved);
			return saved;
		}

		if (isLegacyHousingOnly(legacyHousing.getId(), activeSubcategories)) {
			legacyHousing.setActive(false);
			categoryRepository.save(householdId, legacyHousing);
			subcategoryRepository.desativarPorCategoriaId(householdId, legacyHousing.getId());
			categoriesByName.remove(normalize(LEGACY_HOUSING_CATEGORY));
		}
		return currentHousing;
	}

	private void deactivateLegacyGeneralCategory(
		Long householdId,
		Map<String, Category> categoriesByName,
		List<Subcategory> activeSubcategories
	) {
		Category legacyGeneral = categoriesByName.get(normalize(LEGACY_GENERAL_CATEGORY));
		if (legacyGeneral == null || !isLegacyGeneralOnly(legacyGeneral.getId(), activeSubcategories)) {
			return;
		}

		legacyGeneral.setActive(false);
		categoryRepository.save(householdId, legacyGeneral);
		subcategoryRepository.desativarPorCategoriaId(householdId, legacyGeneral.getId());
		categoriesByName.remove(normalize(LEGACY_GENERAL_CATEGORY));
	}

	private void deactivateLegacyGroceryUnderHousing(Long householdId, Long housingCategoryId, List<Subcategory> activeSubcategories) {
		for (Subcategory subcategory : activeSubcategories) {
			if (!subcategory.getCategoryId().equals(housingCategoryId)) {
				continue;
			}
			if (!normalize(LEGACY_GROCERY_SUBCATEGORY).equals(normalize(subcategory.getName()))) {
				continue;
			}
			subcategory.setActive(false);
			subcategoryRepository.save(householdId, subcategory);
		}
	}

	private void ensureMinimumCategories(Long householdId, Map<String, Category> categoriesByName) {
		for (String categoryName : DEFAULT_CATEGORY_ORDER) {
			Category existing = categoryRepository.findByNameIgnoreCase(householdId, categoryName).orElse(null);
			if (existing == null) {
				Category created = categoryRepository.save(householdId, new Category(null, categoryName, true));
				categoriesByName.put(normalize(created.getName()), created);
				continue;
			}
			if (!existing.isActive()) {
				existing.setActive(true);
				existing = categoryRepository.save(householdId, existing);
			}
			categoriesByName.put(normalize(existing.getName()), existing);
		}
	}

	private void ensureMinimumSubcategories(Long householdId, Map<String, Category> categoriesByName) {
		Map<Long, Set<String>> activeSubcategoriesByCategoryId = subcategoryRepository.findActiveByHouseholdId(householdId).stream()
			.collect(Collectors.groupingBy(
				Subcategory::getCategoryId,
				Collectors.mapping(subcategory -> normalize(subcategory.getName()), Collectors.toSet())
			));

		for (Map.Entry<String, List<String>> entry : DEFAULT_TAXONOMY.entrySet()) {
			Category category = categoriesByName.get(normalize(entry.getKey()));
			if (category == null) {
				continue;
			}
			Set<String> activeNames = activeSubcategoriesByCategoryId.getOrDefault(category.getId(), Set.of());
			for (String subcategoryName : entry.getValue()) {
				if (activeNames.contains(normalize(subcategoryName))) {
					continue;
				}
				subcategoryRepository.save(householdId, new Subcategory(null, category.getId(), subcategoryName, true));
			}
		}
	}

	private boolean isLegacyGeneralOnly(Long categoryId, List<Subcategory> activeSubcategories) {
		Set<String> names = activeSubcategories.stream()
			.filter(subcategory -> subcategory.getCategoryId().equals(categoryId))
			.map(Subcategory::getName)
			.map(this::normalize)
			.collect(Collectors.toSet());
		return names.isEmpty() || names.equals(Set.of(normalize(LEGACY_GENERAL_SUBCATEGORY)));
	}

	private boolean isLegacyHousingOnly(Long categoryId, List<Subcategory> activeSubcategories) {
		Set<String> names = activeSubcategories.stream()
			.filter(subcategory -> subcategory.getCategoryId().equals(categoryId))
			.map(Subcategory::getName)
			.map(this::normalize)
			.collect(Collectors.toSet());
		return names.isEmpty() || names.stream().allMatch(name ->
			name.equals(normalize("Internet")) || name.equals(normalize(LEGACY_GROCERY_SUBCATEGORY))
		);
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static Map<String, List<String>> defaultTaxonomy() {
		Map<String, List<String>> taxonomy = new LinkedHashMap<>();
		taxonomy.put("Moradia", List.of("Aluguel", "Condomínio", "Energia", "Água", "Internet"));
		taxonomy.put("Alimentação", List.of("Mercado", "Restaurante"));
		taxonomy.put("Transporte", List.of("Combustível", "Transporte público"));
		taxonomy.put("Trabalho", List.of("Equipamentos", "Deslocamento"));
		taxonomy.put("Saúde", List.of("Consulta", "Farmácia"));
		taxonomy.put("Pessoal", List.of("Higiene", "Roupas"));
		taxonomy.put("Lazer", List.of("Passeios", "Viagem"));
		taxonomy.put("Serviços e Assinaturas", List.of("Celular", "Streaming", "Hospedagem", "IA"));
		taxonomy.put("Financeiro / Obrigações", List.of("Impostos", "Taxas"));
		taxonomy.put("Outros", List.of("Outros"));
		return taxonomy;
	}
}
