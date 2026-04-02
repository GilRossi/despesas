package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

@ExtendWith(MockitoExtension.class)
class FinancialAssistantRecommendationServiceTest {

	@Mock
	private FinancialAssistantAnalyticsService analyticsService;

	@Mock
	private FinancialAssistantInsightsService insightsService;

	@Mock
	private FinancialAssistantAccessContextProvider accessContextProvider;

	private FinancialAssistantRecommendationService service;

	@BeforeEach
	void setUp() {
		service = new FinancialAssistantRecommendationService(analyticsService, insightsService, accessContextProvider);
		when(accessContextProvider.requireContext()).thenReturn(new FinancialAssistantAccessContext(7L, 11L, "OWNER"));
	}

	@Test
	void deve_gerar_recomendacoes_com_base_em_concentracao_aumento_e_recorrencia() {
		YearMonth month = YearMonth.of(2026, 3);
		FinancialAssistantAccessContext context = new FinancialAssistantAccessContext(7L, 11L, "OWNER");
		when(analyticsService.summarize(context, month.atDay(1), month.atEndOfMonth())).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			3,
			new BigDecimal("1000.00"),
			new BigDecimal("400.00"),
			new BigDecimal("600.00"),
			"Moradia",
			List.of(
				new CategorySpendingResponse(10L, "Moradia", new BigDecimal("600.00"), 2, new BigDecimal("60.00"))
			),
				List.of(
					new TopExpenseResponse(1L, "Aluguel", new BigDecimal("500.00"), LocalDate.of(2026, 3, 5), "Moradia", "Aluguel", null)
				)
			));
		when(insightsService.insights(context, month)).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("1000.00"), "2026-02", new BigDecimal("700.00"), new BigDecimal("300.00"), new BigDecimal("42.86")),
			List.of(new IncreaseAlertResponse("Moradia", new BigDecimal("600.00"), new BigDecimal("300.00"), new BigDecimal("300.00"), new BigDecimal("100.00"))),
			List.of(new RecurringExpenseResponse("Internet", "Moradia", "Internet", new BigDecimal("120.00"), 3, true, LocalDate.of(2026, 3, 10)))
		));

		FinancialAssistantRecommendationsResponse response = service.recommendations(month);

		assertEquals(4, response.recommendations().size());
		assertTrue(response.recommendations().getFirst().title().contains("Revise"));
	}

	@Test
	void deve_retornar_recomendacao_de_sem_dados() {
		YearMonth month = YearMonth.of(2026, 3);
		FinancialAssistantAccessContext context = new FinancialAssistantAccessContext(7L, 11L, "OWNER");
		when(analyticsService.summarize(context, month.atDay(1), month.atEndOfMonth())).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			0,
			new BigDecimal("0.00"),
			new BigDecimal("0.00"),
			new BigDecimal("0.00"),
			null,
				List.of(),
				List.of()
			));
		when(insightsService.insights(context, month)).thenReturn(new FinancialAssistantInsightsResponse(null, List.of(), List.of()));

		FinancialAssistantRecommendationsResponse response = service.recommendations(month);

		assertEquals(1, response.recommendations().size());
		assertEquals("Sem dados suficientes", response.recommendations().getFirst().title());
		assertEquals(
			"Ainda não há despesas registradas neste período para gerar sugestões úteis.",
			response.recommendations().getFirst().rationale()
		);
		assertEquals(
			"Registre despesas do seu espaço para liberar comparações e próximos passos.",
			response.recommendations().getFirst().action()
		);
	}
}
