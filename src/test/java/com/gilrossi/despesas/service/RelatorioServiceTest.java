package com.gilrossi.despesas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantIntent;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryMode;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse;
import com.gilrossi.despesas.financialassistant.IncreaseAlertResponse;
import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;
import com.gilrossi.despesas.financialassistant.RecurringExpenseResponse;
import com.gilrossi.despesas.financialassistant.TopExpenseResponse;

class RelatorioServiceTest {

	@Mock
	private FinancialAssistantAnalyticsService analyticsService;

	@Mock
	private FinancialAssistantInsightsService insightsService;

	@Mock
	private FinancialAssistantRecommendationService recommendationService;

	@Mock
	private FinancialAssistantQueryService queryService;

	private RelatorioService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new RelatorioService(analyticsService, insightsService, recommendationService, queryService);
	}

	@Test
	void deve_montar_relatorio_com_comparacao_e_breakdown() {
		FinancialAssistantPeriodSummaryResponse currentSummary = summary(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			4,
			new BigDecimal("3360.00"),
			new BigDecimal("1520.00"),
			new BigDecimal("1840.00"),
			List.of(
				new CategorySpendingResponse(1L, "Moradia", new BigDecimal("1320.00"), 2, new BigDecimal("39.29")),
				new CategorySpendingResponse(2L, "Alimentação", new BigDecimal("830.00"), 2, new BigDecimal("24.70"))
			)
		);
		FinancialAssistantPeriodSummaryResponse previousSummary = summary(
			LocalDate.of(2026, 2, 1),
			LocalDate.of(2026, 2, 28),
			3,
			new BigDecimal("2180.00"),
			new BigDecimal("990.00"),
			new BigDecimal("1190.00"),
			List.of(
				new CategorySpendingResponse(1L, "Moradia", new BigDecimal("1200.00"), 1, new BigDecimal("55.05")),
				new CategorySpendingResponse(2L, "Alimentação", new BigDecimal("570.00"), 1, new BigDecimal("26.14"))
			)
		);
		FinancialAssistantInsightsResponse insights = new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("3360.00"), "2026-02", new BigDecimal("2180.00"), new BigDecimal("1180.00"), new BigDecimal("54.13")),
			List.of(new IncreaseAlertResponse("Alimentação", new BigDecimal("830.00"), new BigDecimal("570.00"), new BigDecimal("260.00"), new BigDecimal("45.61"))),
			List.of(new RecurringExpenseResponse("Aluguel", "Moradia", "Casa", new BigDecimal("1200.00"), 3, true, LocalDate.of(2026, 3, 10)))
		);
		FinancialAssistantRecommendationsResponse recommendations = new FinancialAssistantRecommendationsResponse(List.of(
			new RecommendationResponse("Revise moradia", "Categoria segue dominante.", "Negocie despesas fixas.")
		));

		when(analyticsService.summarize(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(currentSummary);
		when(analyticsService.summarize(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))).thenReturn(previousSummary);
		when(insightsService.insights(YearMonth.of(2026, 3))).thenReturn(insights);
		when(recommendationService.recommendations(YearMonth.of(2026, 3))).thenReturn(recommendations);

		var relatorio = service.carregarPagina("2026-03", true);

		assertThat(relatorio.referenceMonth()).isEqualTo(YearMonth.of(2026, 3));
		assertThat(relatorio.showComparison()).isTrue();
		assertThat(relatorio.highestCategoryName()).isEqualTo("Moradia");
		assertThat(relatorio.categoryBreakdown()).hasSize(2);
		assertThat(relatorio.categoryBreakdown().getFirst().deltaAmount()).isEqualByComparingTo("120.00");
		assertThat(relatorio.topExpenses()).hasSize(1);
		assertThat(relatorio.increaseAlerts()).hasSize(1);
	}

	@Test
	void deve_montar_relatorio_vazio_sem_alertas_quando_nao_houver_dados() {
		FinancialAssistantPeriodSummaryResponse emptySummary = summary(
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 4, 30),
			0,
			new BigDecimal("0.00"),
			new BigDecimal("0.00"),
			new BigDecimal("0.00"),
			List.of()
		);
		FinancialAssistantInsightsResponse insights = new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-04", new BigDecimal("0.00"), "2026-03", new BigDecimal("3360.00"), new BigDecimal("-3360.00"), new BigDecimal("-100.00")),
			List.of(),
			List.of()
		);
		FinancialAssistantRecommendationsResponse recommendations = new FinancialAssistantRecommendationsResponse(List.of(
			new RecommendationResponse("Sem dados suficientes", "Nao ha despesas registradas.", "Registre novas despesas.")
		));

		when(analyticsService.summarize(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))).thenReturn(emptySummary);
		when(analyticsService.summarize(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			4,
			new BigDecimal("3360.00"),
			new BigDecimal("1520.00"),
			new BigDecimal("1840.00"),
			List.of(new CategorySpendingResponse(1L, "Moradia", new BigDecimal("1320.00"), 2, new BigDecimal("39.29")))
		));
		when(insightsService.insights(YearMonth.of(2026, 4))).thenReturn(insights);
		when(recommendationService.recommendations(YearMonth.of(2026, 4))).thenReturn(recommendations);

		var relatorio = service.carregarPagina("2026-04", false);

		assertThat(relatorio.hasData()).isFalse();
		assertThat(relatorio.showComparison()).isFalse();
		assertThat(relatorio.categoryBreakdown()).isEmpty();
		assertThat(relatorio.recommendations()).hasSize(1);
	}

	@Test
	void deve_executar_atalho_do_assistente_com_mes_resolvido() {
		when(queryService.ask(new com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03")))
			.thenReturn(new FinancialAssistantQueryResponse(
				"Como posso economizar este mês?",
				FinancialAssistantQueryMode.AI,
				FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS,
				"Revise os gastos variáveis.",
				null,
				null,
				null,
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				null
			));

		var resposta = service.executarAtalho("COMO_ECONOMIZAR", "2026-03");

		assertThat(resposta.aiGenerated()).isTrue();
		assertThat(resposta.answer()).contains("Revise");
		verify(queryService).ask(new com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03"));
	}

	@Test
	void deve_rejeitar_periodo_invalido() {
		assertThatThrownBy(() -> service.carregarPagina("03-2026", true))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Período inválido. Use o formato yyyy-MM.");
	}

	private FinancialAssistantPeriodSummaryResponse summary(
		LocalDate from,
		LocalDate to,
		long totalExpenses,
		BigDecimal totalAmount,
		BigDecimal paidAmount,
		BigDecimal remainingAmount,
		List<CategorySpendingResponse> categories
	) {
		return new FinancialAssistantPeriodSummaryResponse(
			from,
			to,
			totalExpenses,
			totalAmount,
			paidAmount,
			remainingAmount,
			categories.isEmpty() ? null : categories.getFirst().categoryName(),
			categories,
			List.of(new TopExpenseResponse(7L, "Aluguel", new BigDecimal("1200.00"), to.minusDays(4), "Moradia", "Casa", null))
		);
	}
}
