package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantGatewayException;
import com.gilrossi.despesas.ratelimit.AbuseProtectionService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class FinancialAssistantQueryServiceHardeningTest {

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
	private AbuseProtectionService abuseProtectionService;

	private FinancialAssistantAuditLogger auditLogger;

	private FinancialAssistantQueryService service;
	private ListAppender<ILoggingEvent> logAppender;
	private ListAppender<ILoggingEvent> auditLogAppender;

	@BeforeEach
	void setUp() {
		auditLogger = new FinancialAssistantAuditLogger();
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
		Logger logger = (Logger) LoggerFactory.getLogger(FinancialAssistantQueryService.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
		Logger auditLogger = (Logger) LoggerFactory.getLogger(FinancialAssistantAuditLogger.class);
		auditLogAppender = new ListAppender<>();
		auditLogAppender.start();
		auditLogger.addAppender(auditLogAppender);
	}

	@AfterEach
	void tearDown() {
		Logger logger = (Logger) LoggerFactory.getLogger(FinancialAssistantQueryService.class);
		logger.detachAppender(logAppender);
		Logger auditLogger = (Logger) LoggerFactory.getLogger(FinancialAssistantAuditLogger.class);
		auditLogger.detachAppender(auditLogAppender);
	}

	@Test
	void deve_degradar_timeout_sem_expor_payload_do_provedor_no_log() {
		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new RuntimeException(new java.net.http.HttpTimeoutException("provider timeout with payload"))));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("TIMEOUT", "provider timeout with payload");
	}

	@Test
	void deve_degradar_erro_de_rede_sem_expor_payload_no_log() {
		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new RuntimeException(new java.net.ConnectException("connection refused to provider.internal"))));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("NETWORK_ERROR", "provider.internal");
	}

	@Test
	void deve_degradar_erro_de_autenticacao_do_provedor_com_log_sanitizado() {
		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new ProviderAuthenticationException("{\"error\":{\"message\":\"Authentication Fails\",\"type\":\"authentication_error\"}}")));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("AUTH_ERROR", "authentication_error");
	}

	@Test
	void deve_degradar_erro_remoto_do_provedor_com_log_sanitizado() {
		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new ProviderServerException("{\"error\":{\"message\":\"server exploded\",\"type\":\"server_error\"}}")));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("PROVIDER_ERROR", "server_error");
	}

	@Test
	void deve_auditar_inicio_e_conclusao_sem_expor_pergunta_ou_resposta() {
		when(conversationGateway.isAvailable()).thenReturn(false);

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertThat(auditLogAppender.list).isNotEmpty();
		String messages = auditLogAppender.list.stream()
			.map(ILoggingEvent::getFormattedMessage)
			.reduce("", (left, right) -> left + "\n" + right);
		assertThat(messages).contains("event=assistant_query_started");
		assertThat(messages).contains("event=assistant_query_completed");
		assertThat(messages).contains("householdId=11");
		assertThat(messages).contains("mode=FALLBACK");
		assertThat(messages).doesNotContain("Como posso economizar este mês?");
		assertThat(messages).doesNotContain("A IA sugeriu");
	}

	@Test
	void deve_negar_contexto_sem_household_ativo_com_auditoria_sem_vazamento() {
		when(accessContextProvider.requireContext()).thenThrow(FinancialAssistantContextException.invalidHouseholdScope(9L, "PLATFORM_ADMIN"));

		FinancialAssistantContextException exception = assertThrows(
			FinancialAssistantContextException.class,
			() -> service.ask(new FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03"))
		);

		assertThat(exception.reasonCode()).isEqualTo("ASSISTANT_INVALID_HOUSEHOLD_CONTEXT");
		String message = auditLogAppender.list.getLast().getFormattedMessage();
		assertThat(message).contains("event=assistant_query_denied");
		assertThat(message).contains("userId=9");
		assertThat(message).contains("role=PLATFORM_ADMIN");
		assertThat(message).doesNotContain("Como posso economizar este mês?");
	}

	private FinancialAssistantQueryResponse askInterpretativeQuestion() {
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
		return service.ask(request);
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

	private void assertSanitizedLog(String expectedCategory, String forbiddenFragment) {
		assertThat(logAppender.list).isNotEmpty();
		String formattedMessage = logAppender.list.getLast().getFormattedMessage();
		assertThat(formattedMessage).contains("category=" + expectedCategory);
		assertThat(formattedMessage).doesNotContain(forbiddenFragment);
		assertThat(formattedMessage).doesNotContain("sk-");
		assertThat(formattedMessage).doesNotContain("question");
	}

	private static class ProviderAuthenticationException extends RuntimeException {
		ProviderAuthenticationException(String message) {
			super(message);
		}
	}

	private static class ProviderServerException extends RuntimeException {
		ProviderServerException(String message) {
			super(message);
		}
	}
}
