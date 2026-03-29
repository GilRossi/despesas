package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FinancialAssistantAiPropertiesCoverageTest {

	@Test
	void deve_expor_setters_getters_e_status_de_configuracao() {
		FinancialAssistantAiProperties properties = new FinancialAssistantAiProperties();

		assertFalse(properties.isConfigured());

		properties.setEnabled(true);
		properties.setApiKey("secret-key");
		properties.setBaseUrl("https://llm.local/v1");
		properties.setModel("deepseek-reasoner");
		properties.setLogRequests(true);
		properties.setLogResponses(true);
		properties.setTemperature(0.4d);
		properties.setMaxCompletionTokens(512);
		properties.setTimeoutSeconds(45);
		properties.setMaxRetries(3);

		assertTrue(properties.isEnabled());
		assertEquals("secret-key", properties.getApiKey());
		assertEquals("https://llm.local/v1", properties.getBaseUrl());
		assertEquals("deepseek-reasoner", properties.getModel());
		assertTrue(properties.isLogRequests());
		assertTrue(properties.isLogResponses());
		assertEquals(0.4d, properties.getTemperature());
		assertEquals(512, properties.getMaxCompletionTokens());
		assertEquals(45, properties.getTimeoutSeconds());
		assertEquals(3, properties.getMaxRetries());
		assertTrue(properties.isConfigured());
	}
}
