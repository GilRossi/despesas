package com.gilrossi.despesas.financialassistant.ai;

public record FinancialAssistantConversationRequest(
	String question,
	String referenceMonth,
	String resolvedIntent,
	String resolvedCategoryName
) {
}
