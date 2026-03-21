package com.gilrossi.despesas.emailingestion;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.emailingestion.persistence.EmailIngestionSourceJpaEntity;
import com.gilrossi.despesas.emailingestion.persistence.EmailIngestionSourceJpaRepository;

@Repository
public class JpaEmailIngestionSourceRepositoryAdapter implements EmailIngestionSourceRepository {

	private final EmailIngestionSourceJpaRepository repository;

	public JpaEmailIngestionSourceRepositoryAdapter(EmailIngestionSourceJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmailIngestionSource> findAllByHouseholdId(Long householdId) {
		return repository.findAllByHouseholdIdOrderBySourceAccountAsc(householdId).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailIngestionSource> findByNormalizedSourceAccount(String normalizedSourceAccount) {
		return repository.findByNormalizedSourceAccount(normalizedSourceAccount).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailIngestionSource> findActiveByNormalizedSourceAccount(String normalizedSourceAccount) {
		return repository.findByNormalizedSourceAccountAndActiveTrue(normalizedSourceAccount).map(this::toDomain);
	}

	@Override
	@Transactional
	public EmailIngestionSource save(EmailIngestionSource source) {
		EmailIngestionSourceJpaEntity entity = source.id() == null
			? new EmailIngestionSourceJpaEntity()
			: repository.findById(source.id()).orElseGet(EmailIngestionSourceJpaEntity::new);
		entity.setId(source.id());
		entity.setHouseholdId(source.householdId());
		entity.setSourceAccount(source.sourceAccount());
		entity.setNormalizedSourceAccount(source.normalizedSourceAccount());
		entity.setLabel(source.label());
		entity.setActive(source.active());
		entity.setAutoImportMinConfidence(source.autoImportMinConfidence());
		entity.setReviewMinConfidence(source.reviewMinConfidence());
		EmailIngestionSourceJpaEntity saved = repository.saveAndFlush(entity);
		return toDomain(saved);
	}

	private EmailIngestionSource toDomain(EmailIngestionSourceJpaEntity entity) {
		return new EmailIngestionSource(
			entity.getId(),
			entity.getHouseholdId(),
			entity.getSourceAccount(),
			entity.getNormalizedSourceAccount(),
			entity.getLabel(),
			entity.isActive(),
			entity.getAutoImportMinConfidence(),
			entity.getReviewMinConfidence(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}
}
