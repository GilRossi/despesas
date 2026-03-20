package com.gilrossi.despesas.catalog.category;

import java.util.Locale;
import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.catalog.category.persistence.CategoryJpaEntity;
import com.gilrossi.despesas.catalog.category.persistence.CategoryJpaRepository;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

	private final CategoryJpaRepository repository;

	public JpaCategoryRepositoryAdapter(CategoryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Category> findAll(Long householdId, String q, Boolean active, Pageable pageable) {
		return repository.findAllByFilters(householdId, normalizeQuery(q), active, pageable).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findById(Long householdId, Long id) {
		return repository.findByIdAndHouseholdIdAndDeletedAtIsNull(householdId, id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findByNameIgnoreCase(Long householdId, String name) {
		if (!StringUtils.hasText(name)) {
			return Optional.empty();
		}
		return repository.findByNameIgnoreCaseAndHouseholdIdAndDeletedAtIsNull(householdId, name.trim()).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(Long householdId, String name, Long id) {
		if (!StringUtils.hasText(name)) {
			return false;
		}
		return repository.existsByNameIgnoreCaseAndIdNotAndHouseholdIdAndActiveTrue(householdId, name.trim(), id);
	}

	@Override
	@Transactional
	public Category save(Long householdId, Category category) {
		CategoryJpaEntity entity = category.getId() == null
			? new CategoryJpaEntity()
			: repository.findByIdAndHouseholdIdAndDeletedAtIsNull(householdId, category.getId()).orElseGet(CategoryJpaEntity::new);

		entity.setId(category.getId());
		entity.setHouseholdId(householdId);
		entity.setName(category.getName() == null ? null : category.getName().trim());
		entity.setActive(category.isActive());

		CategoryJpaEntity saved = repository.saveAndFlush(entity);
		return toDomain(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Category> findActiveByHouseholdId(Long householdId) {
		return repository.findActiveByHouseholdId(householdId).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional
	public void deleteAll() {
		repository.deleteAllInBatch();
		repository.flush();
	}

	private String normalizeQuery(String q) {
		return q == null ? null : q.trim().toLowerCase(Locale.ROOT);
	}

	private Category toDomain(CategoryJpaEntity entity) {
		return new Category(entity.getId(), entity.getName(), entity.isActive());
	}
}
