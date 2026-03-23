package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationRequest;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationResult;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationGateway;

@ExtendWith(MockitoExtension.class)
class FinancialAssistantQueryServiceTest {

	@Mock
	private FinancialAssistantIntentResolver intentResolver;

	@Mock
	private FinancialAssistantAccessContextProvider accessContextProvider;

	@Mock
	private FinancialAssistantAnalyticsService analyticsService;

	@Mock
	private FinancialAssistantInsightsService insightsService;

	@Mock
	private FinancialAssistantRecommendationService recommendationService;

	@Mock
	private FinancialAssistantConversationGateway conversationGateway;

	@Mock
	private FinancialAssistantAuditLogger auditLogger;

	private FinancialAssistantQueryService service;

	@BeforeEach
	void setUp() {
		service = new FinancialAssistantQueryService(
			intentResolver,
			accessContextProvider,
			analyticsService,
			insightsService,
			recommendationService,
			conversationGateway,
			auditLogger
		);
		when(accessContextProvider.requireContext()).thenReturn(new FinancialAssistantAccessContext(7L, 11L, "OWNER"));
	}

	@Test
	void deve_responder_em_fallback_quando_ia_estiver_indisponivel() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Onde estou gastando mais?", "2026-03");
		when(intentResolver.resolve("Onde estou gastando mais?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.HIGHEST_SPENDING_CATEGORY, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(false);

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertEquals(FinancialAssistantIntent.HIGHEST_SPENDING_CATEGORY, response.intent());
		assertEquals("Moradia", response.highestSpendingCategory().categoryName());
		assertNull(response.aiUsage());
	}

	@Test
	void deve_usar_gateway_de_ia_quando_disponivel_e_intencao_for_interpretativa() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03");
		when(intentResolver.resolve("Como posso economizar este mês?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(any())).thenReturn(
			new FinancialAssistantConversationResult(
				"A IA sugeriu economias para o período.",
				new FinancialAssistantAiUsage("deepseek-chat", 100, 40, 140, 80, 20, 2, "STOP")
			)
		);

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.AI, response.mode());
		assertEquals("A IA sugeriu economias para o período.", response.answer());
		assertEquals(140, response.aiUsage().totalTokens());
		ArgumentCaptor<FinancialAssistantConversationRequest> captor = ArgumentCaptor.forClass(FinancialAssistantConversationRequest.class);
		verify(conversationGateway).answer(captor.capture());
		assertEquals("Como posso economizar este mês?", captor.getValue().question());
		assertEquals("2026-03", captor.getValue().referenceMonth());
		assertEquals("SAVINGS_RECOMMENDATIONS", captor.getValue().resolvedIntent());
		assertEquals("", captor.getValue().resolvedCategoryName());
	}

	@Test
	void deve_economizar_tokens_e_nao_chamar_ia_para_intencao_direta() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Onde estou gastando mais?", "2026-03");
		when(intentResolver.resolve("Onde estou gastando mais?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.HIGHEST_SPENDING_CATEGORY, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(true);

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		verify(conversationGateway, never()).answer(any());
	}

	@Test
	void deve_economizar_tokens_e_nao_chamar_ia_para_intencao_desconhecida() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Me ajuda?", "2026-03");
		when(intentResolver.resolve("Me ajuda?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.UNKNOWN, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(true);

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertEquals(FinancialAssistantIntent.UNKNOWN, response.intent());
		assertNull(response.aiUsage());
		verify(conversationGateway, never()).answer(any());
	}

	@Test
	void deve_economizar_tokens_e_nao_chamar_ia_quando_nao_houver_dados_no_periodo() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03");
		when(intentResolver.resolve("Como posso economizar este mês?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			0,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null,
			List.of(),
			List.of()
		));
		when(analyticsService.topExpenses(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3), 5)).thenReturn(List.of());
		when(insightsService.insights(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", BigDecimal.ZERO, "2026-02", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(new FinancialAssistantAccessContext(7L, 11L, "OWNER"), YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(true);

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		verify(conversationGateway, never()).answer(any());
	}

	private FinancialAssistantPeriodSummaryResponse summary() {
		return new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			2,
			new BigDecimal("300.00"),
			new BigDecimal("100.00"),
			new BigDecimal("200.00"),
			"Moradia",
			List.of(new CategorySpendingResponse(10L, "Moradia", new BigDecimal("200.00"), 1, new BigDecimal("66.67"))),
			List.of(new TopExpenseResponse(1L, "Aluguel", new BigDecimal("200.00"), LocalDate.of(2026, 3, 5), "Moradia", "Aluguel", null))
		);
	}
}
