package com.gilrossi.despesas.model;

import com.gilrossi.despesas.financialassistant.FinancialAssistantIntent;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryMode;

public record RelatorioAssistenteResposta(
	String label,
	String question,
	String answer,
	FinancialAssistantQueryMode mode,
	FinancialAssistantIntent intent
) {

	public boolean aiGenerated() {
		return mode == FinancialAssistantQueryMode.AI;
	}
}
