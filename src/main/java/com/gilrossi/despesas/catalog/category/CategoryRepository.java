package com.gilrossi.despesas.catalog.category;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryRepository {

	Page<Category> findAll(Long householdId, String q, Boolean active, Pageable pageable);

	Optional<Category> findById(Long householdId, Long id);

	Optional<Category> findByNameIgnoreCase(Long householdId, String name);

	boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(Long householdId, String name, Long id);

	Category save(Long householdId, Category category);

	List<Category> findActiveByHouseholdId(Long householdId);

	void deleteAll();
}
