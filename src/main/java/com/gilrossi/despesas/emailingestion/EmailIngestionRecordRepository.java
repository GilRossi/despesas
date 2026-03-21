package com.gilrossi.despesas.emailingestion;

import java.util.List;
import java.util.Optional;

public interface EmailIngestionRecordRepository {

	Optional<EmailIngestionRecord> findBySourceAccountAndExternalMessageId(String normalizedSourceAccount, String externalMessageId);

	Optional<EmailIngestionRecord> findLatestByHouseholdIdAndFingerprint(Long householdId, String fingerprint);

	Optional<EmailIngestionRecord> findByIdAndHouseholdId(Long id, Long householdId);

	List<EmailIngestionRecord> findAllByHouseholdId(Long householdId);

	List<EmailIngestionRecord> findAllPendingReviewByHouseholdId(Long householdId);

	EmailIngestionRecord save(EmailIngestionRecord record);
}
