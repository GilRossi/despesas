package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FinancialAssistantAiPropertiesTest {

	@Test
	void deve_manter_defaults_de_baixo_custo() {
		FinancialAssistantAiProperties properties = new FinancialAssistantAiProperties();

		assertThat(properties.getTemperature()).isEqualTo(0.1d);
		assertThat(properties.getMaxCompletionTokens()).isEqualTo(220);
		assertThat(properties.getTimeoutSeconds()).isEqualTo(20);
		assertThat(properties.getMaxRetries()).isEqualTo(1);
		assertThat(properties.getModel()).isEqualTo("deepseek-chat");
	}
}
