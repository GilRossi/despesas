package com.gilrossi.despesas.financialassistant.ai;

public interface FinancialAssistantConversationGateway {

	boolean isAvailable();

	FinancialAssistantConversationResult answer(FinancialAssistantConversationRequest request);
}
