package com.gilrossi.despesas.spacereference;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.spacereference.persistence.SpaceReferenceJpaEntity;
import com.gilrossi.despesas.spacereference.persistence.SpaceReferenceJpaRepository;

@Repository
public class JpaSpaceReferenceRepositoryAdapter implements SpaceReferenceRepository {

	private final SpaceReferenceJpaRepository repository;

	public JpaSpaceReferenceRepositoryAdapter(SpaceReferenceJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SpaceReference> findAll(Long householdId, SpaceReferenceTypeGroup typeGroup, SpaceReferenceType type, String q) {
		String normalizedQuery = SpaceReferenceNormalizer.normalizeQuery(q);
		if (type != null) {
			return repository.findAllByFilters(householdId, type, normalizedQuery).stream()
				.map(this::toDomain)
				.toList();
		}
		if (typeGroup != null) {
			return repository.findAllByTypeInAndFilters(householdId, SpaceReferenceType.fromGroup(typeGroup), normalizedQuery).stream()
				.map(this::toDomain)
				.toList();
		}
		return repository.findAllByFilters(householdId, null, normalizedQuery).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SpaceReference> findByTypeAndNormalizedName(Long householdId, SpaceReferenceType type, String normalizedName) {
		return repository.findByHouseholdIdAndTypeAndNormalizedNameAndDeletedAtIsNull(householdId, type, normalizedName)
			.map(this::toDomain);
	}

	@Override
	@Transactional
	public SpaceReference save(SpaceReference reference) {
		SpaceReferenceJpaEntity entity = reference.getId() == null
			? new SpaceReferenceJpaEntity()
			: repository.findByIdAndHouseholdIdAndDeletedAtIsNull(reference.getId(), reference.getHouseholdId())
				.orElseGet(SpaceReferenceJpaEntity::new);

		entity.setId(reference.getId());
		entity.setHouseholdId(reference.getHouseholdId());
		entity.setType(reference.getType());
		entity.setName(reference.getName());
		entity.setNormalizedName(reference.getNormalizedName());
		entity.setDeletedAt(reference.getDeletedAt());

		return toDomain(repository.saveAndFlush(entity));
	}

	@Override
	@Transactional
	public void deleteAll() {
		repository.deleteAllInBatch();
		repository.flush();
	}

	private SpaceReference toDomain(SpaceReferenceJpaEntity entity) {
		return new SpaceReference(
			entity.getId(),
			entity.getHouseholdId(),
			entity.getType(),
			entity.getName(),
			entity.getNormalizedName(),
			entity.getCreatedAt(),
			entity.getUpdatedAt(),
			entity.getDeletedAt()
		);
	}
}
