package com.gilrossi.despesas.emailingestion.persistence;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmailIngestionRecordJpaRepository extends JpaRepository<EmailIngestionRecordJpaEntity, Long> {

	Optional<EmailIngestionRecordJpaEntity> findByNormalizedSourceAccountAndExternalMessageId(String normalizedSourceAccount, String externalMessageId);

	Optional<EmailIngestionRecordJpaEntity> findFirstByHouseholdIdAndFingerprintOrderByCreatedAtDescIdDesc(Long householdId, String fingerprint);

	Optional<EmailIngestionRecordJpaEntity> findByIdAndHouseholdId(Long id, Long householdId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select record
		from EmailIngestionRecordJpaEntity record
		where record.id = :id
		  and record.householdId = :householdId
		""")
	Optional<EmailIngestionRecordJpaEntity> findByIdAndHouseholdIdForUpdate(Long id, Long householdId);

	List<EmailIngestionRecordJpaEntity> findAllByHouseholdIdOrderByCreatedAtDescIdDesc(Long householdId);

	List<EmailIngestionRecordJpaEntity> findAllByHouseholdIdAndFinalDecisionOrderByCreatedAtDescIdDesc(
		Long householdId,
		com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision finalDecision
	);
}
