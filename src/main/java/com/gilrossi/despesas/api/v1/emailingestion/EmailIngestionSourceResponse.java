package com.gilrossi.despesas.api.v1.emailingestion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.gilrossi.despesas.emailingestion.EmailIngestionSource;

public record EmailIngestionSourceResponse(
	Long id,
	String sourceAccount,
	String label,
	boolean active,
	BigDecimal autoImportMinConfidence,
	BigDecimal reviewMinConfidence,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {
	public static EmailIngestionSourceResponse from(EmailIngestionSource source) {
		return new EmailIngestionSourceResponse(
			source.id(),
			source.sourceAccount(),
			source.label(),
			source.active(),
			source.autoImportMinConfidence(),
			source.reviewMinConfidence(),
			source.createdAt(),
			source.updatedAt()
		);
	}
}
