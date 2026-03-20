package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class FinancialAssistantIntentResolverTest {

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	@Mock
	private CategoryRepository categoryRepository;

	private FinancialAssistantIntentResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new FinancialAssistantIntentResolver(currentHouseholdProvider, categoryRepository);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
		when(categoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(
			new Category(10L, "Alimentação", true),
			new Category(11L, "Moradia", true)
		));
	}

	@Test
	void deve_resolver_total_por_categoria_no_periodo() {
		ResolvedFinancialAssistantQuery response = resolver.resolve("Quanto gastei com alimentacao este mes?", YearMonth.of(2026, 3));

		assertEquals(FinancialAssistantIntent.TOTAL_BY_CATEGORY_IN_PERIOD, response.intent());
		assertEquals("Alimentação", response.categoryName());
		assertEquals(YearMonth.of(2026, 3), response.referenceMonth());
	}

	@Test
	void deve_resolver_alerta_de_aumento_com_mes_passado() {
		ResolvedFinancialAssistantQuery response = resolver.resolve("O que aumentou em relação ao mês passado?", YearMonth.of(2026, 3));

		assertEquals(FinancialAssistantIntent.INCREASE_ALERTS, response.intent());
		assertEquals(YearMonth.of(2026, 3), response.referenceMonth());
	}

	@Test
	void deve_retroceder_referencia_para_consultas_de_periodo_no_mes_passado() {
		ResolvedFinancialAssistantQuery response = resolver.resolve("Quanto gastei com alimentacao no mês passado?", YearMonth.of(2026, 3));

		assertEquals(FinancialAssistantIntent.TOTAL_BY_CATEGORY_IN_PERIOD, response.intent());
		assertEquals("Alimentação", response.categoryName());
		assertEquals(YearMonth.of(2026, 2), response.referenceMonth());
	}

	@Test
	void deve_marcar_pergunta_ambigua_como_unknown() {
		ResolvedFinancialAssistantQuery response = resolver.resolve("Me ajuda a entender minhas finanças", YearMonth.of(2026, 3));

		assertEquals(FinancialAssistantIntent.UNKNOWN, response.intent());
	}
}
