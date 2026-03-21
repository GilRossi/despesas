package com.gilrossi.despesas.emailingestion;

import java.util.List;
import java.util.Optional;

public interface EmailIngestionSourceRepository {

	List<EmailIngestionSource> findAllByHouseholdId(Long householdId);

	Optional<EmailIngestionSource> findByNormalizedSourceAccount(String normalizedSourceAccount);

	Optional<EmailIngestionSource> findActiveByNormalizedSourceAccount(String normalizedSourceAccount);

	EmailIngestionSource save(EmailIngestionSource source);
}
