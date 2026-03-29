package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantGatewayException;
import com.gilrossi.despesas.ratelimit.AbuseProtectionService;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;
import com.gilrossi.despesas.ratelimit.RateLimitScope;

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

	@Mock
	private AbuseProtectionService abuseProtectionService;

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
			auditLogger,
			abuseProtectionService
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

	@Test
	void deve_reportar_contexto_invalido_quando_nao_houver_escopo_de_household() {
		when(accessContextProvider.requireContext()).thenThrow(FinancialAssistantContextException.invalidHouseholdScope(7L, "OWNER"));
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Resumo", "2026-03");

		FinancialAssistantContextException exception = assertThrows(FinancialAssistantContextException.class, () -> service.ask(request));

		assertEquals("ASSISTANT_INVALID_HOUSEHOLD_CONTEXT", exception.reasonCode());
		verify(auditLogger).queryDenied(exception);
	}

	@Test
	void deve_reportar_rate_limit_antes_de_processar_a_pergunta() {
		doThrow(new RateLimitExceededException(RateLimitScope.ASSISTANT_QUERY, 1, 300, 120))
			.when(abuseProtectionService)
			.checkAssistantQuery(new FinancialAssistantAccessContext(7L, 11L, "OWNER"));
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Resumo", "2026-03");

		assertThrows(RateLimitExceededException.class, () -> service.ask(request));
		verify(auditLogger).queryRateLimited(
			eq(new FinancialAssistantAccessContext(7L, 11L, "OWNER")),
			eq("2026-03"),
			any(RateLimitExceededException.class)
		);
	}

	@Test
	void deve_retorno_fallback_para_categoria_em_branco_e_nao_disparar_consulta_adicional() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Quanto gastei?", "2026-03");
		FinancialAssistantAccessContext context = new FinancialAssistantAccessContext(7L, 11L, "OWNER");
		when(intentResolver.resolve("Quanto gastei?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.TOTAL_BY_CATEGORY_IN_PERIOD, YearMonth.of(2026, 3), " "));
		when(analyticsService.summarize(context, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(context, YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(context, YearMonth.of(2026, 3))).thenReturn(insightsForMonth());
		when(recommendationService.recommendations(context, YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(false);

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertEquals("Nao identifiquei a categoria na pergunta. Informe a categoria desejada para eu calcular o total do periodo.", response.answer());
		verify(analyticsService, never()).totalByCategory(any(), any());
	}

	@Test
	void deve_cobrir_caminhos_de_resposta_com_dados_reais_sem_ia() {
		FinancialAssistantAccessContext context = new FinancialAssistantAccessContext(7L, 11L, "OWNER");
		when(conversationGateway.isAvailable()).thenReturn(false);
		when(recommendationService.recommendations(any(FinancialAssistantAccessContext.class), any(YearMonth.class)))
			.thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));

		when(intentResolver.resolve("top", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.TOP_EXPENSES_IN_PERIOD, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(context, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(context, YearMonth.of(2026, 3), 5)).thenReturn(List.of());
		when(insightsService.insights(context, YearMonth.of(2026, 3))).thenReturn(insightsForMonth());
		assertEquals(
			"Nao ha despesas registradas em 2026-03 para listar os maiores gastos.",
			service.ask(new FinancialAssistantQueryRequest("top", "2026-03")).answer()
		);

		when(intentResolver.resolve("rec", YearMonth.of(2026, 4), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.RECURRING_EXPENSES, YearMonth.of(2026, 4), null));
		when(analyticsService.summarize(context, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))).thenReturn(summary());
		when(analyticsService.topExpenses(context, YearMonth.of(2026, 4), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(context, YearMonth.of(2026, 4))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-04", new BigDecimal("300.00"), "2026-03", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		assertEquals(
			"Nao encontrei despesas com sinal forte de recorrencia no historico analisado.",
			service.ask(new FinancialAssistantQueryRequest("rec", "2026-04")).answer()
		);

		when(intentResolver.resolve("alerts", YearMonth.of(2026, 5), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.INCREASE_ALERTS, YearMonth.of(2026, 5), null));
		when(analyticsService.summarize(context, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(context, YearMonth.of(2026, 5), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(context, YearMonth.of(2026, 5))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-05", new BigDecimal("300.00"), "2026-04", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		assertEquals(
			"Nao encontrei aumentos relevantes por categoria no comparativo com o mes anterior.",
			service.ask(new FinancialAssistantQueryRequest("alerts", "2026-05")).answer()
		);

		when(intentResolver.resolve("summary", YearMonth.of(2026, 6), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.PERIOD_SUMMARY, YearMonth.of(2026, 6), null));
		when(analyticsService.summarize(context, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30),
			0,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			null,
			List.of(),
			List.of()
		));
		when(analyticsService.topExpenses(context, YearMonth.of(2026, 6), 5)).thenReturn(List.of());
		when(insightsService.insights(context, YearMonth.of(2026, 6))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-06", BigDecimal.ZERO, "2026-05", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
			List.of(),
			List.of()
		));
		assertEquals("Nao ha despesas registradas em 2026-06.", service.ask(new FinancialAssistantQueryRequest("summary", "2026-06")).answer());
	}

	@Test
	void deve_usar_ia_e_cair_em_fallback_quando_gateway_disparar_excecao() {
		FinancialAssistantAccessContext context = new FinancialAssistantAccessContext(7L, 11L, "OWNER");
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Como posso economizar?", "2026-03");
		when(intentResolver.resolve("Como posso economizar?", YearMonth.of(2026, 3), 11L))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(context, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(context, YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(context, YearMonth.of(2026, 3))).thenReturn(insightsForMonth());
		when(recommendationService.recommendations(context, YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of(new RecommendationResponse("Cortar gastos", "Reduzir assinaturas", "Cortar gastos"))));
		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(any())).thenThrow(FinancialAssistantGatewayException.from(new IllegalStateException("boom")));

		FinancialAssistantQueryResponse response = service.ask(request);

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
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

	private FinancialAssistantInsightsResponse insightsForMonth() {
		return new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(new IncreaseAlertResponse("Moradia", new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("100.00"))),
			List.of(new RecurringExpenseResponse("Aluguel", "Moradia", "Aluguel", new BigDecimal("200.00"), 3, true, LocalDate.of(2026, 3, 31)))
		);
	}
}
