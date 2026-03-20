package com.gilrossi.despesas.financialassistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.TopExpenseResponse;

@ExtendWith(MockitoExtension.class)
class LangChain4jFinancialAssistantToolsTest {

	@Mock
	private FinancialAssistantAnalyticsService analyticsService;

	@Mock
	private FinancialAssistantInsightsService insightsService;

	@Mock
	private FinancialAssistantRecommendationService recommendationService;

	@Test
	void deve_limitar_top_expenses_a_cinco_itens() {
		LangChain4jFinancialAssistantTools tools = new LangChain4jFinancialAssistantTools(
			analyticsService,
			insightsService,
			recommendationService
		);
		when(analyticsService.topExpenses(YearMonth.of(2026, 3), 5)).thenReturn(List.of(
			topExpense("Despesa 1", 1),
			topExpense("Despesa 2", 2),
			topExpense("Despesa 3", 3),
			topExpense("Despesa 4", 4),
			topExpense("Despesa 5", 5),
			topExpense("Despesa 6", 6)
		));

		String response = tools.topExpenses("2026-03", 10);

		assertThat(response).contains("Despesa 5");
		assertThat(response).doesNotContain("Despesa 6");
	}

	@Test
	void deve_limitar_period_summary_a_tres_categorias_e_tres_despesas() {
		LangChain4jFinancialAssistantTools tools = new LangChain4jFinancialAssistantTools(
			analyticsService,
			insightsService,
			recommendationService
		);
		when(analyticsService.summarize(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			6,
			new BigDecimal("1200.00"),
			new BigDecimal("400.00"),
			new BigDecimal("800.00"),
			"Moradia",
			List.of(
				new CategorySpendingResponse(1L, "Moradia", new BigDecimal("500.00"), 1, new BigDecimal("41.67")),
				new CategorySpendingResponse(2L, "Alimentação", new BigDecimal("300.00"), 2, new BigDecimal("25.00")),
				new CategorySpendingResponse(3L, "Lazer", new BigDecimal("200.00"), 1, new BigDecimal("16.67")),
				new CategorySpendingResponse(4L, "Saúde", new BigDecimal("100.00"), 1, new BigDecimal("8.33"))
			),
			List.of(
				topExpense("Despesa 1", 1),
				topExpense("Despesa 2", 2),
				topExpense("Despesa 3", 3),
				topExpense("Despesa 4", 4)
			)
		));

		String response = tools.periodSummary("2026-03");

		assertThat(response).contains("Moradia:500.00");
		assertThat(response).contains("Lazer:200.00");
		assertThat(response).doesNotContain("Saúde:100.00");
		assertThat(response).contains("Despesa 3");
		assertThat(response).doesNotContain("Despesa 4");
	}

	private TopExpenseResponse topExpense(String description, long id) {
		return new TopExpenseResponse(id, description, new BigDecimal(100 * id), LocalDate.of(2026, 3, (int) id), "Categoria", "Subcategoria", null);
	}
}
