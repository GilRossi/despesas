package com.gilrossi.despesas.api.v1.emailingestion;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailIngestionSourceRequest(
	@NotBlank(message = "sourceAccount must not be blank")
	@Size(max = 160, message = "sourceAccount must have at most 160 characters")
	String sourceAccount,
	@Size(max = 120, message = "label must have at most 120 characters")
	String label,
	@DecimalMin(value = "0.0", message = "autoImportMinConfidence must be between 0 and 1")
	@DecimalMax(value = "1.0", message = "autoImportMinConfidence must be between 0 and 1")
	BigDecimal autoImportMinConfidence,
	@DecimalMin(value = "0.0", message = "reviewMinConfidence must be between 0 and 1")
	@DecimalMax(value = "1.0", message = "reviewMinConfidence must be between 0 and 1")
	BigDecimal reviewMinConfidence
) {
}
