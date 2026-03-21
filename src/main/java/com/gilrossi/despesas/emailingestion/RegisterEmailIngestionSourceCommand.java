package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;

public record RegisterEmailIngestionSourceCommand(
	String sourceAccount,
	String label,
	BigDecimal autoImportMinConfidence,
	BigDecimal reviewMinConfidence
) {
}
