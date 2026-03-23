package com.gilrossi.despesas.financialassistant;

public record FinancialAssistantAccessContext(
	Long userId,
	Long householdId,
	String role
) {
}
