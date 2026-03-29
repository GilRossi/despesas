package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationRequest;
import com.gilrossi.despesas.financialassistant.ai.UnavailableFinancialAssistantConversationGateway;

class UnavailableFinancialAssistantConversationGatewayTest {

	@Test
	void deve_reportar_gateway_indisponivel_e_falhar_com_categoria_estavel() {
		UnavailableFinancialAssistantConversationGateway gateway = new UnavailableFinancialAssistantConversationGateway();

		assertThat(gateway.isAvailable()).isFalse();
		assertThatThrownBy(() -> gateway.answer(new FinancialAssistantConversationRequest(
			"Resumo",
			"2026-03",
			"PERIOD_SUMMARY",
			""
		)))
			.isInstanceOf(com.gilrossi.despesas.financialassistant.ai.FinancialAssistantGatewayException.class)
			.hasMessage("Financial assistant provider call failed");
	}
}
