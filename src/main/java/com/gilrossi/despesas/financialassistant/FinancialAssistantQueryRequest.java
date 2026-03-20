package com.gilrossi.despesas.financialassistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinancialAssistantQueryRequest(
	@NotBlank(message = "question must not be blank")
	@Size(max = 500, message = "question must have at most 500 characters")
	String question,
	String referenceMonth
) {
}
