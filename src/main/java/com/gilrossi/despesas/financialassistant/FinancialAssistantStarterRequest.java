package com.gilrossi.despesas.financialassistant;

import jakarta.validation.constraints.NotNull;

public record FinancialAssistantStarterRequest(
	@NotNull(message = "intent must not be null")
	FinancialAssistantStarterIntent intent
) {
}
