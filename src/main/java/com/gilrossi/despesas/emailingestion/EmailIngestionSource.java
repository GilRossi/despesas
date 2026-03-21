package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EmailIngestionSource(
	Long id,
	Long householdId,
	String sourceAccount,
	String normalizedSourceAccount,
	String label,
	boolean active,
	BigDecimal autoImportMinConfidence,
	BigDecimal reviewMinConfidence,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {
}
