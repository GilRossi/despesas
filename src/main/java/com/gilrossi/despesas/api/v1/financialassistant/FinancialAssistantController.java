package com.gilrossi.despesas.api.v1.financialassistant;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantKpisResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantStarterRequest;
import com.gilrossi.despesas.financialassistant.FinancialAssistantStarterResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantStarterService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantSupport;

@RestController
@RequestMapping("/api/v1/financial-assistant")
public class FinancialAssistantController {

	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantInsightsService insightsService;
	private final FinancialAssistantRecommendationService recommendationService;
	private final FinancialAssistantQueryService queryService;
	private final FinancialAssistantStarterService starterService;

	public FinancialAssistantController(
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantInsightsService insightsService,
		FinancialAssistantRecommendationService recommendationService,
		FinancialAssistantQueryService queryService,
		FinancialAssistantStarterService starterService
	) {
		this.analyticsService = analyticsService;
		this.insightsService = insightsService;
		this.recommendationService = recommendationService;
		this.queryService = queryService;
		this.starterService = starterService;
	}

	@GetMapping("/summary")
	public ApiResponse<FinancialAssistantPeriodSummaryResponse> summary(
		@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to
	) {
		return new ApiResponse<>(analyticsService.summarize(from, to));
	}

	@GetMapping("/kpis")
	public ApiResponse<FinancialAssistantKpisResponse> kpis(
		@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to
	) {
		return new ApiResponse<>(analyticsService.kpis(from, to));
	}

	@GetMapping("/insights")
	public ApiResponse<FinancialAssistantInsightsResponse> insights(@RequestParam(required = false) String referenceMonth) {
		return new ApiResponse<>(insightsService.insights(FinancialAssistantSupport.resolveReferenceMonth(referenceMonth)));
	}

	@GetMapping("/recommendations")
	public ApiResponse<FinancialAssistantRecommendationsResponse> recommendations(@RequestParam(required = false) String referenceMonth) {
		return new ApiResponse<>(recommendationService.recommendations(FinancialAssistantSupport.resolveReferenceMonth(referenceMonth)));
	}

	@PostMapping("/query")
	public ApiResponse<FinancialAssistantQueryResponse> query(@Valid @RequestBody FinancialAssistantQueryRequest request) {
		return new ApiResponse<>(queryService.ask(request));
	}

	@PostMapping("/starter-intent")
	public ApiResponse<FinancialAssistantStarterResponse> starterIntent(@Valid @RequestBody FinancialAssistantStarterRequest request) {
		return new ApiResponse<>(starterService.respond(request.intent()));
	}
}
