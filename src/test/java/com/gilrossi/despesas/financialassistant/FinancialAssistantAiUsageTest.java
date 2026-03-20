package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FinancialAssistantAiUsageTest {

	@Test
	void deve_aceitar_usage_consistente() {
		FinancialAssistantAiUsage usage = new FinancialAssistantAiUsage("deepseek-chat", 100, 30, 130, 80, 20, 1, "STOP");

		assertEquals(130, usage.totalTokens());
		assertEquals(20, usage.promptCacheMissTokens());
	}

	@Test
	void deve_rejeitar_total_inconsistente() {
		assertThrows(IllegalArgumentException.class, () -> new FinancialAssistantAiUsage(
			"deepseek-chat",
			100,
			30,
			120,
			80,
			20,
			1,
			"STOP"
		));
	}

	@Test
	void deve_rejeitar_cache_inconsistente() {
		assertThrows(IllegalArgumentException.class, () -> new FinancialAssistantAiUsage(
			"deepseek-chat",
			100,
			30,
			130,
			80,
			30,
			1,
			"STOP"
		));
	}
}
