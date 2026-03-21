package com.gilrossi.despesas.emailingestion.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailIngestionSourceJpaRepository extends JpaRepository<EmailIngestionSourceJpaEntity, Long> {

	List<EmailIngestionSourceJpaEntity> findAllByHouseholdIdOrderBySourceAccountAsc(Long householdId);

	Optional<EmailIngestionSourceJpaEntity> findByNormalizedSourceAccount(String normalizedSourceAccount);

	Optional<EmailIngestionSourceJpaEntity> findByNormalizedSourceAccountAndActiveTrue(String normalizedSourceAccount);
}
