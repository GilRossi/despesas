package com.gilrossi.despesas.financialassistant.ai;

import com.gilrossi.despesas.financialassistant.FinancialAssistantAiUsage;

public record FinancialAssistantConversationResult(
	String answer,
	FinancialAssistantAiUsage usage
) {
}
