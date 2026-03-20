package com.gilrossi.despesas.api.v1.financialassistant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAiUsage;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantKpiResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantKpisResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryMode;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantIntent;
import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;

@WebMvcTest(FinancialAssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class FinancialAssistantControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FinancialAssistantAnalyticsService analyticsService;

	@MockitoBean
	private FinancialAssistantInsightsService insightsService;

	@MockitoBean
	private FinancialAssistantRecommendationService recommendationService;

	@MockitoBean
	private FinancialAssistantQueryService queryService;

	@Test
	void deve_retornar_summary_do_assistente_financeiro() throws Exception {
		when(analyticsService.summarize(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(new FinancialAssistantPeriodSummaryResponse(
			LocalDate.of(2026, 3, 1),
			LocalDate.of(2026, 3, 31),
			2,
			new BigDecimal("300.00"),
			new BigDecimal("100.00"),
			new BigDecimal("200.00"),
			"Moradia",
			List.of(new CategorySpendingResponse(10L, "Moradia", new BigDecimal("200.00"), 1, new BigDecimal("66.67"))),
			List.of()
		));

		mockMvc.perform(get("/api/v1/financial-assistant/summary")
				.param("from", "2026-03-01")
				.param("to", "2026-03-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalAmount").value(300.00))
			.andExpect(jsonPath("$.data.highestSpendingCategory").value("Moradia"));
	}

	@Test
	void deve_retornar_kpis_e_insights() throws Exception {
		when(analyticsService.kpis(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(new FinancialAssistantKpisResponse(
			List.of(new FinancialAssistantKpiResponse("TOTAL_SPENT", "Total", new BigDecimal("300.00"), "descricao"))
		));
		when(insightsService.insights(java.time.YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantInsightsResponse(
			new MonthComparisonResponse("2026-03", new BigDecimal("300.00"), "2026-02", new BigDecimal("200.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
			List.of(),
			List.of()
		));
		when(recommendationService.recommendations(java.time.YearMonth.of(2026, 3))).thenReturn(new FinancialAssistantRecommendationsResponse(
			List.of(new RecommendationResponse("Revise", "Motivo", "Ação"))
		));

		mockMvc.perform(get("/api/v1/financial-assistant/kpis")
				.param("from", "2026-03-01")
				.param("to", "2026-03-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.kpis[0].code").value("TOTAL_SPENT"));

		mockMvc.perform(get("/api/v1/financial-assistant/insights")
				.param("referenceMonth", "2026-03"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.monthComparison.currentMonth").value("2026-03"));

		mockMvc.perform(get("/api/v1/financial-assistant/recommendations")
				.param("referenceMonth", "2026-03"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.recommendations[0].title").value("Revise"));
	}

	@Test
	void deve_retornar_query_ai_com_ai_usage_no_contrato() throws Exception {
		when(queryService.ask(new com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest("Como posso economizar este mês?", "2026-03"))).thenReturn(
			new FinancialAssistantQueryResponse(
				"Como posso economizar este mês?",
				FinancialAssistantQueryMode.AI,
				FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS,
				"A IA sugeriu economias para o período.",
				null,
				null,
				null,
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				new FinancialAssistantAiUsage("deepseek-chat", 100, 30, 130, 80, 20, 1, "STOP")
			)
		);

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Como posso economizar este mês?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.question").value("Como posso economizar este mês?"))
			.andExpect(jsonPath("$.data.mode").value("AI"))
			.andExpect(jsonPath("$.data.intent").value("SAVINGS_RECOMMENDATIONS"))
			.andExpect(jsonPath("$.data.answer").value("A IA sugeriu economias para o período."))
			.andExpect(jsonPath("$.data.aiUsage.totalTokens").value(130));
	}

	@Test
	void deve_retornar_query_fallback_sem_ai_usage_para_intencao_direta() throws Exception {
		when(queryService.ask(new com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest("Onde estou gastando mais?", "2026-03"))).thenReturn(
			new FinancialAssistantQueryResponse(
				"Onde estou gastando mais?",
				FinancialAssistantQueryMode.FALLBACK,
				FinancialAssistantIntent.HIGHEST_SPENDING_CATEGORY,
				"Moradia lidera os gastos.",
				null,
				null,
				new CategorySpendingResponse(10L, "Moradia", new BigDecimal("200.00"), 1, new BigDecimal("66.67")),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				null
			)
		);

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Onde estou gastando mais?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("FALLBACK"))
			.andExpect(jsonPath("$.data.intent").value("HIGHEST_SPENDING_CATEGORY"))
			.andExpect(jsonPath("$.data.highestSpendingCategory.categoryName").value("Moradia"))
			.andExpect(jsonPath("$.data.aiUsage").isEmpty());
	}

	@Test
	void deve_retornar_query_fallback_unknown_com_payload_minimo_estavel() throws Exception {
		when(queryService.ask(new com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest("Qual carro devo comprar?", "2026-03"))).thenReturn(
			new FinancialAssistantQueryResponse(
				"Qual carro devo comprar?",
				FinancialAssistantQueryMode.FALLBACK,
				FinancialAssistantIntent.UNKNOWN,
				"Nao consegui identificar uma intencao financeira especifica.",
				null,
				null,
				null,
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				null
			)
		);

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Qual carro devo comprar?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.question").value("Qual carro devo comprar?"))
			.andExpect(jsonPath("$.data.mode").value("FALLBACK"))
			.andExpect(jsonPath("$.data.intent").value("UNKNOWN"))
			.andExpect(jsonPath("$.data.answer").value("Nao consegui identificar uma intencao financeira especifica."))
			.andExpect(jsonPath("$.data.aiUsage").isEmpty());
	}

	@Test
	void deve_validar_question_obrigatoria_no_endpoint_de_query() throws Exception {
		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"   "
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
