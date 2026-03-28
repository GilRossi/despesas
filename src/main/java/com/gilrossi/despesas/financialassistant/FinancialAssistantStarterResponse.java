package com.gilrossi.despesas.financialassistant;

public record FinancialAssistantStarterResponse(
	FinancialAssistantStarterIntent intent,
	FinancialAssistantStarterKind kind,
	String title,
	String message,
	String primaryActionKey
) {

	public static FinancialAssistantStarterResponse starter(
		FinancialAssistantStarterIntent intent,
		String title,
		String message,
		String primaryActionKey
	) {
		return new FinancialAssistantStarterResponse(
			intent,
			FinancialAssistantStarterKind.STARTER,
			title,
			message,
			primaryActionKey
		);
	}
}
