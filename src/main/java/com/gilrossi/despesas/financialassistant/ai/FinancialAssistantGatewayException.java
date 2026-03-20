package com.gilrossi.despesas.financialassistant.ai;

import com.gilrossi.despesas.financialassistant.FinancialAssistantAiFailureCategory;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAiFailureClassifier;

public class FinancialAssistantGatewayException extends RuntimeException {

	private final FinancialAssistantAiFailureCategory category;

	private FinancialAssistantGatewayException(FinancialAssistantAiFailureCategory category, Throwable cause) {
		super("Financial assistant provider call failed", cause);
		this.category = category;
	}

	public static FinancialAssistantGatewayException from(Throwable throwable) {
		return new FinancialAssistantGatewayException(
			FinancialAssistantAiFailureClassifier.classify(throwable),
			throwable
		);
	}

	public FinancialAssistantAiFailureCategory category() {
		return category;
	}
}
