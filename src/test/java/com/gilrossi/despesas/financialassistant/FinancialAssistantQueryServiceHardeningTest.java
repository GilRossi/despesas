package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class FinancialAssistantQueryServiceHardeningTest {

	@Mock
	private FinancialAssistantIntentResolver intentResolver;

	@Mock
	private FinancialAssistantAnalyticsService analyticsService;

	@Mock
	private FinancialAssistantInsightsService insightsService;

	@Mock
	private FinancialAssistantRecommendationService recommendationService;

	@Mock
	private FinancialAssistantConversationGateway conversationGateway;

	private FinancialAssistantQueryService service;
	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		service = new FinancialAssistantQueryService(intentResolver, analyticsService, insightsService, recommendationService, conversationGateway);
		Logger logger = (Logger) LoggerFactory.getLogger(FinancialAssistantQueryService.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		Logger logger = (Logger) LoggerFactory.getLogger(FinancialAssistantQueryService.class);
		logger.detachAppender(logAppender);
	}

	@Test
	void deve_degradar_timeout_sem_expor_payload_do_provedor_no_log() {
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new RuntimeException(new java.net.http.HttpTimeoutException("provider timeout with payload"))));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("TIMEOUT", "provider timeout with payload");
	}

	@Test
	void deve_degradar_erro_de_rede_sem_expor_payload_no_log() {
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new RuntimeException(new java.net.ConnectException("connection refused to provider.internal"))));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("NETWORK_ERROR", "provider.internal");
	}

	@Test
	void deve_degradar_erro_de_autenticacao_do_provedor_com_log_sanitizado() {
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new ProviderAuthenticationException("{\"error\":{\"message\":\"Authentication Fails\",\"type\":\"authentication_error\"}}")));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("AUTH_ERROR", "authentication_error");
	}

	@Test
	void deve_degradar_erro_remoto_do_provedor_com_log_sanitizado() {
		when(conversationGateway.answer(org.mockito.ArgumentMatchers.any()))
			.thenThrow(FinancialAssistantGatewayException.from(new ProviderServerException("{\"error\":{\"message\":\"server exploded\",\"type\":\"server_error\"}}")));

		FinancialAssistantQueryResponse response = askInterpretativeQuestion();

		assertEquals(FinancialAssistantQueryMode.FALLBACK, response.mode());
		assertNull(response.aiUsage());
		assertSanitizedLog("PROVIDER_ERROR", "server_error");
	}

	private FinancialAssistantQueryResponse askInterpretativeQuestion() {
		FinancialAssistantQueryRequest request = new FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03");
		when(intentResolver.resolve("Como posso economizar este mês?", YearMonth.of(2026, 3)))
			.thenReturn(new ResolvedFinancialAssistantQuery(FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS, YearMonth.of(2026, 3), null));
		when(analyticsService.summarize(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(summary());
		when(analyticsService.topExpenses(YearMonth.of(2026, 3), 5)).thenReturn(summary().topExpenses());
		when(insightsService.insights(YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(List.of()));
		when(conversationGateway.isAvailable()).thenReturn(true);
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
