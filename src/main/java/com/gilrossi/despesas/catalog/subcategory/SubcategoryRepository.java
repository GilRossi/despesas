package com.gilrossi.despesas.catalog.subcategory;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubcategoryRepository {

	Page<Subcategory> findAll(Long householdId, Long categoryId, String q, Boolean active, Pageable pageable);

	Optional<Subcategory> findById(Long householdId, Long id);

	boolean existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(Long householdId, Long categoryId, String name, Long id);

	Subcategory save(Long householdId, Subcategory subcategory);

	List<Subcategory> findActiveByHouseholdId(Long householdId);

	void desativarPorCategoriaId(Long householdId, Long categoryId);

	void deleteAll();
}
