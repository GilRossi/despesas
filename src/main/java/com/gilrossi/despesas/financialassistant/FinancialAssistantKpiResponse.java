package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;

public record FinancialAssistantKpiResponse(
	String code,
	String label,
	BigDecimal amount,
	String description
) {
}
