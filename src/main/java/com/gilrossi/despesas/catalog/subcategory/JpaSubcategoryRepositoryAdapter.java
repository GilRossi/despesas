package com.gilrossi.despesas.catalog.subcategory;

import java.util.Locale;
import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.catalog.subcategory.persistence.SubcategoryJpaEntity;
import com.gilrossi.despesas.catalog.subcategory.persistence.SubcategoryJpaRepository;

@Repository
public class JpaSubcategoryRepositoryAdapter implements SubcategoryRepository {

	private final SubcategoryJpaRepository repository;

	public JpaSubcategoryRepositoryAdapter(SubcategoryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Subcategory> findAll(Long householdId, Long categoryId, String q, Boolean active, Pageable pageable) {
		return repository.findAllByFilters(householdId, categoryId, normalizeQuery(q), active, pageable).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Subcategory> findById(Long householdId, Long id) {
		return repository.findByIdAndHouseholdIdAndDeletedAtIsNull(householdId, id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(Long householdId, Long categoryId, String name, Long id) {
		if (!StringUtils.hasText(name)) {
			return false;
		}
		return repository.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(householdId, categoryId, name.trim(), id);
	}

	@Override
	@Transactional
	public Subcategory save(Long householdId, Subcategory subcategory) {
		SubcategoryJpaEntity entity = subcategory.getId() == null
			? new SubcategoryJpaEntity()
			: repository.findByIdAndHouseholdIdAndDeletedAtIsNull(householdId, subcategory.getId()).orElseGet(SubcategoryJpaEntity::new);

		entity.setId(subcategory.getId());
		entity.setHouseholdId(householdId);
		entity.setCategoryId(subcategory.getCategoryId());
		entity.setName(subcategory.getName() == null ? null : subcategory.getName().trim());
		entity.setActive(subcategory.isActive());

		SubcategoryJpaEntity saved = repository.saveAndFlush(entity);
		return toDomain(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Subcategory> findActiveByHouseholdId(Long householdId) {
		return repository.findActiveByHouseholdId(householdId).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional
	public void desativarPorCategoriaId(Long householdId, Long categoryId) {
		repository.desativarPorCategoriaId(householdId, categoryId);
		repository.flush();
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

	private Subcategory toDomain(SubcategoryJpaEntity entity) {
		return new Subcategory(entity.getId(), entity.getCategoryId(), entity.getName(), entity.isActive());
	}
}
