package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.gilrossi.despesas.financialassistant.ai.DeepSeekFinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.LangChain4jFinancialAssistantTools;
import com.gilrossi.despesas.financialassistant.ai.UnavailableFinancialAssistantConversationGateway;

class FinancialAssistantAiConfigTest {

	@Test
	void deve_retornar_gateway_indisponivel_quando_ai_estiver_desabilitada() {
		FinancialAssistantAiConfig config = new FinancialAssistantAiConfig();
		FinancialAssistantAiProperties properties = new FinancialAssistantAiProperties();

		FinancialAssistantConversationGateway gateway = config.financialAssistantConversationGateway(
			properties,
			Mockito.mock(LangChain4jFinancialAssistantTools.class)
		);

		assertThat(gateway).isInstanceOf(UnavailableFinancialAssistantConversationGateway.class);
		assertThat(gateway.isAvailable()).isFalse();
	}

	@Test
	void deve_retornar_gateway_real_quando_ai_estiver_habilitada_e_configurada() {
		FinancialAssistantAiConfig config = new FinancialAssistantAiConfig();
		FinancialAssistantAiProperties properties = new FinancialAssistantAiProperties();
		properties.setEnabled(true);
		properties.setApiKey("deepseek-local-key");
		properties.setBaseUrl("https://llm.local/v1");
		properties.setModel("deepseek-chat");
		properties.setTemperature(0.2d);
		properties.setMaxCompletionTokens(333);
		properties.setTimeoutSeconds(12);
		properties.setMaxRetries(2);

		FinancialAssistantConversationGateway gateway = config.financialAssistantConversationGateway(
			properties,
			Mockito.mock(LangChain4jFinancialAssistantTools.class)
		);

		assertThat(gateway).isInstanceOf(DeepSeekFinancialAssistantConversationGateway.class);
		assertThat(gateway.isAvailable()).isTrue();
	}
}
