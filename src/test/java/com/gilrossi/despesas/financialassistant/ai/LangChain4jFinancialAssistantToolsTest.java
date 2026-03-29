package com.gilrossi.despesas.financialassistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.IncreaseAlertResponse;
import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecurringExpenseResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;
import com.gilrossi.despesas.financialassistant.TopExpenseResponse;

@ExtendWith(MockitoExtension.class)
class LangChain4jFinancialAssistantToolsTest {

	@Mock
	private FinancialAssistantAnalyticsService analyticsService;

	@Mock
	private FinancialAssistantInsightsService insightsService;

	@Mock
	private FinancialAssistantRecommendationService recommendationService;

	private LangChain4jFinancialAssistantTools tools;

	@BeforeEach
	void setUp() {
		tools = new LangChain4jFinancialAssistantTools(analyticsService, insightsService, recommendationService);
	}

	@Test
	void deve_formatar_resumo_e_analises_do_mes() {
		YearMonth month = YearMonth.of(2026, 3);
		when(analyticsService.summarize(month.atDay(1), month.atEndOfMonth())).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			month.atDay(1),
			month.atEndOfMonth(),
			2,
			new BigDecimal("320.00"),
			new BigDecimal("50.00"),
			new BigDecimal("270.00"),
			"Moradia",
			List.of(new CategorySpendingResponse(1L, "Moradia", new BigDecimal("200.00"), 2, new BigDecimal("62.50"))),
			List.of(new TopExpenseResponse(10L, "Internet", new BigDecimal("120.00"), LocalDate.of(2026, 3, 31), "Moradia", "Internet", ExpenseContext.CASA))
		));
		when(analyticsService.totalByCategory(month, "Moradia")).thenReturn(new BigDecimal("200.00"));
		when(analyticsService.highestSpendingCategory(month)).thenReturn(new CategorySpendingResponse(1L, "Moradia", new BigDecimal("200.00"), 2, new BigDecimal("62.50")));
		when(analyticsService.topExpenses(month, 3)).thenReturn(List.of(new TopExpenseResponse(10L, "Internet", new BigDecimal("120.00"), LocalDate.of(2026, 3, 31), "Moradia", "Internet", ExpenseContext.CASA)));
		when(analyticsService.compareMonths(month)).thenReturn(new MonthComparisonResponse("2026-03", new BigDecimal("320.00"), "2026-02", new BigDecimal("250.00"), new BigDecimal("70.00"), new BigDecimal("28.00")));
		when(analyticsService.increaseAlerts(month)).thenReturn(List.of(new IncreaseAlertResponse("Moradia", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"))));
		when(analyticsService.recurringExpenses(month)).thenReturn(List.of(new RecurringExpenseResponse("Internet", "Moradia", "Internet", new BigDecimal("120.00"), 3, true, LocalDate.of(2026, 3, 31))));
		when(recommendationService.recommendations(month)).thenReturn(new com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse(
			List.of(new RecommendationResponse("Cortar gastos", "Corte", "Reduzir assinaturas"))
		));

		assertThat(tools.periodSummary("2026-03")).contains("mes=2026-03", "total=320.00", "pagas=50.00", "pendentes=270.00");
		assertThat(tools.totalByCategory("Moradia", "2026-03")).isEqualTo("mes=2026-03; categoria=Moradia; total=200.00");
		assertThat(tools.highestSpendingCategory("2026-03")).contains("categoria=Moradia", "total=200.00");
		assertThat(tools.topExpenses("2026-03", 3)).contains("top=[Internet|120.00|Moradia|2026-03-31]");
		assertThat(tools.monthOverMonthChange("2026-03")).contains("delta=70.00", "variacao_percentual=28.00");
		assertThat(tools.increaseAlerts("2026-03")).contains("Moradia:+100.00(100.00%)");
		assertThat(tools.recurringExpenses("2026-03")).contains("Internet|media=120.00|ocorrencias=3|fixa=true");
		assertThat(tools.recommendations("2026-03")).contains("Cortar gastos:Reduzir assinaturas");
		assertThat(tools.insights("2026-03")).contains("comparativo={mes_atual=2026-03", "aumentos=[Moradia:+100.00(100.00%)", "recorrencias=[Internet|media=120.00|ocorrencias=3|fixa=true]");
	}

	@Test
	void deve_limitar_e_normalizar_quantidade_em_top_expenses() {
		YearMonth month = YearMonth.of(2026, 4);
		when(analyticsService.topExpenses(month, 5)).thenReturn(List.of(
			new TopExpenseResponse(1L, "A", new BigDecimal("10.00"), LocalDate.of(2026, 4, 1), "C1", "S1", ExpenseContext.CASA),
			new TopExpenseResponse(2L, "B", new BigDecimal("20.00"), LocalDate.of(2026, 4, 2), "C2", "S2", ExpenseContext.CASA)
		));

		assertThat(tools.topExpenses("2026-04", 99)).contains("mes=2026-04");
	}

	@Test
	void deve_usar_mes_atual_quando_referencia_esta_vazia() {
		YearMonth current = YearMonth.now();
		when(analyticsService.compareMonths(current)).thenReturn(new MonthComparisonResponse("current", BigDecimal.ONE, "previous", BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("100.00")));
		when(analyticsService.increaseAlerts(current)).thenReturn(List.of());
		when(analyticsService.recurringExpenses(current)).thenReturn(List.of());

		assertThat(tools.monthOverMonthChange(null)).contains("mes_atual=current");
		assertThat(tools.insights(" ")).contains("comparativo={mes_atual=current");
	}

	@Test
	void deve_tratar_listas_vazias_e_categoria_nula() {
		YearMonth month = YearMonth.of(2026, 5);
		when(analyticsService.highestSpendingCategory(month)).thenReturn(null);
		when(recommendationService.recommendations(month)).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(analyticsService.increaseAlerts(month)).thenReturn(List.of());
		when(analyticsService.recurringExpenses(month)).thenReturn(List.of());
		when(analyticsService.compareMonths(month)).thenReturn(new MonthComparisonResponse("2026-05", BigDecimal.ZERO, "2026-04", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

		assertThat(tools.highestSpendingCategory("2026-05")).isEqualTo("mes=2026-05; categoria_lider=nenhuma");
		assertThat(tools.recommendations("2026-05")).isEqualTo("mes=2026-05; recomendacoes=[]");
		assertThat(tools.increaseAlerts("2026-05")).isEqualTo("mes=2026-05; alertas=[]");
		assertThat(tools.recurringExpenses("2026-05")).isEqualTo("mes=2026-05; recorrencias=[]");
		assertThat(tools.insights("2026-05")).contains("aumentos=[]", "recorrencias=[]");
	}

	@Test
	void deve_normalizar_limite_minimo_e_maximo_em_top_expenses() {
		YearMonth month = YearMonth.of(2026, 6);
		when(analyticsService.topExpenses(month, 3)).thenReturn(List.of(
			new TopExpenseResponse(1L, "Conta 1", new BigDecimal("10.00"), LocalDate.of(2026, 6, 1), "Moradia", "Conta 1", ExpenseContext.CASA)
		));

		assertThat(tools.topExpenses("2026-06", 0)).contains("mes=2026-06", "Conta 1|10.00|Moradia|2026-06-01");
	}
}
