package com.gilrossi.despesas.financialassistant.ai;

public class UnavailableFinancialAssistantConversationGateway implements FinancialAssistantConversationGateway {

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public FinancialAssistantConversationResult answer(FinancialAssistantConversationRequest request) {
		throw FinancialAssistantGatewayException.from(
			new IllegalStateException("Financial assistant AI is disabled or not configured")
		);
	}
}
