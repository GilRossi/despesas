package com.gilrossi.despesas.emailingestion.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailIngestionRecordJpaRepository extends JpaRepository<EmailIngestionRecordJpaEntity, Long> {

	Optional<EmailIngestionRecordJpaEntity> findByNormalizedSourceAccountAndExternalMessageId(String normalizedSourceAccount, String externalMessageId);

	Optional<EmailIngestionRecordJpaEntity> findFirstByHouseholdIdAndFingerprintOrderByCreatedAtDescIdDesc(Long householdId, String fingerprint);

	Optional<EmailIngestionRecordJpaEntity> findByIdAndHouseholdId(Long id, Long householdId);

	List<EmailIngestionRecordJpaEntity> findAllByHouseholdIdOrderByCreatedAtDescIdDesc(Long householdId);

	List<EmailIngestionRecordJpaEntity> findAllByHouseholdIdAndFinalDecisionOrderByCreatedAtDescIdDesc(
		Long householdId,
		com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision finalDecision
	);
}
