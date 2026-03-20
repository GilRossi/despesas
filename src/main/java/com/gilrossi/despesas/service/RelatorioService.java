package com.gilrossi.despesas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryRequest;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantQueryService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantSupport;
import com.gilrossi.despesas.model.RelatorioAssistenteAcao;
import com.gilrossi.despesas.model.RelatorioAssistenteResposta;
import com.gilrossi.despesas.model.RelatorioCategoriaLinha;
import com.gilrossi.despesas.model.RelatorioPagina;

@Service
public class RelatorioService {

	private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");
	private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LOCALE_PT_BR);
	private static final int CATEGORY_LIMIT = 6;
	private static final int INSIGHT_LIMIT = 3;

	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantInsightsService insightsService;
	private final FinancialAssistantRecommendationService recommendationService;
	private final FinancialAssistantQueryService queryService;

	public RelatorioService(
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantInsightsService insightsService,
		FinancialAssistantRecommendationService recommendationService,
		FinancialAssistantQueryService queryService
	) {
		this.analyticsService = analyticsService;
		this.insightsService = insightsService;
		this.recommendationService = recommendationService;
		this.queryService = queryService;
	}

	@Transactional(readOnly = true)
	public RelatorioPagina carregarPagina(String referenceMonthValue, boolean compareWithPrevious) {
		YearMonth referenceMonth = resolveReferenceMonth(referenceMonthValue);
		FinancialAssistantPeriodSummaryResponse currentSummary = analyticsService.summarize(referenceMonth.atDay(1), referenceMonth.atEndOfMonth());
		FinancialAssistantInsightsResponse insights = insightsService.insights(referenceMonth);
		FinancialAssistantRecommendationsResponse recommendations = recommendationService.recommendations(referenceMonth);
		FinancialAssistantPeriodSummaryResponse previousSummary = compareWithPrevious
			? analyticsService.summarize(referenceMonth.minusMonths(1).atDay(1), referenceMonth.minusMonths(1).atEndOfMonth())
			: null;
		List<RelatorioCategoriaLinha> categoryBreakdown = categoryBreakdown(currentSummary, previousSummary);
		RelatorioCategoriaLinha highestCategory = categoryBreakdown.isEmpty() ? null : categoryBreakdown.getFirst();

		return new RelatorioPagina(
			referenceMonth,
			referenceMonth.toString(),
			formatMonthLabel(referenceMonth),
			compareWithPrevious,
			currentSummary.totalExpenses(),
			currentSummary.totalAmount(),
			currentSummary.paidAmount(),
			currentSummary.remainingAmount(),
			highestCategory == null ? "Sem dados" : highestCategory.categoryName(),
			highestCategory == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : highestCategory.totalAmount(),
			highestCategory == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : highestCategory.sharePercentage(),
			insights.monthComparison(),
			comparisonWidth(insights.monthComparison() == null ? BigDecimal.ZERO : insights.monthComparison().currentTotal(),
				insights.monthComparison() == null ? BigDecimal.ZERO : max(insights.monthComparison().currentTotal(), insights.monthComparison().previousTotal())),
			comparisonWidth(insights.monthComparison() == null ? BigDecimal.ZERO : insights.monthComparison().previousTotal(),
				insights.monthComparison() == null ? BigDecimal.ZERO : max(insights.monthComparison().currentTotal(), insights.monthComparison().previousTotal())),
			comparisonWidth(currentSummary.paidAmount(), currentSummary.totalAmount()),
			comparisonWidth(currentSummary.remainingAmount(), currentSummary.totalAmount()),
			categoryBreakdown,
			currentSummary.topExpenses().stream().limit(INSIGHT_LIMIT).toList(),
			insights.recurringExpenses().stream().limit(INSIGHT_LIMIT).toList(),
			compareWithPrevious ? insights.increaseAlerts().stream().limit(INSIGHT_LIMIT).toList() : List.of(),
			recommendations.recommendations().stream().limit(INSIGHT_LIMIT).toList(),
			List.of(RelatorioAssistenteAcao.values())
		);
	}

	@Transactional(readOnly = true)
	public RelatorioAssistenteResposta executarAtalho(String actionCode, String referenceMonthValue) {
		RelatorioAssistenteAcao action = RelatorioAssistenteAcao.fromCode(actionCode);
		YearMonth referenceMonth = resolveReferenceMonth(referenceMonthValue);
		FinancialAssistantQueryResponse response = queryService.ask(new FinancialAssistantQueryRequest(action.question(), referenceMonth.toString()));
		return new RelatorioAssistenteResposta(
			action.label(),
			action.question(),
			response.answer(),
			response.mode(),
			response.intent()
		);
	}

	private YearMonth resolveReferenceMonth(String referenceMonthValue) {
		try {
			return FinancialAssistantSupport.resolveReferenceMonth(referenceMonthValue);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Período inválido. Use o formato yyyy-MM.");
		}
	}

	private List<RelatorioCategoriaLinha> categoryBreakdown(
		FinancialAssistantPeriodSummaryResponse currentSummary,
		FinancialAssistantPeriodSummaryResponse previousSummary
	) {
		Map<String, CategorySpendingResponse> previousByCategory = previousSummary == null
			? Map.of()
			: previousSummary.categoryTotals().stream()
				.collect(LinkedHashMap::new,
					(map, category) -> map.put(FinancialAssistantSupport.normalizeText(category.categoryName()), category),
					Map::putAll);
		BigDecimal maxAmount = currentSummary.categoryTotals().stream()
			.map(CategorySpendingResponse::totalAmount)
			.reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), this::max);

		return currentSummary.categoryTotals().stream()
			.limit(CATEGORY_LIMIT)
			.map(category -> {
				CategorySpendingResponse previous = previousByCategory.get(FinancialAssistantSupport.normalizeText(category.categoryName()));
				BigDecimal previousAmount = previous == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : previous.totalAmount();
				BigDecimal deltaAmount = category.totalAmount().subtract(previousAmount);
				BigDecimal deltaPercentage = FinancialAssistantSupport.percentageChange(category.totalAmount(), previousAmount);
				return new RelatorioCategoriaLinha(
					category.categoryName(),
					category.totalAmount(),
					category.sharePercentage(),
					previousAmount,
					deltaAmount,
					deltaPercentage,
					comparisonWidth(category.totalAmount(), maxAmount)
				);
			})
			.toList();
	}

	private int comparisonWidth(BigDecimal value, BigDecimal total) {
		if (value == null || total == null || total.signum() <= 0) {
			return 0;
		}
		return value.multiply(new BigDecimal("100"))
			.divide(total, 0, RoundingMode.HALF_UP)
			.intValue();
	}

	private BigDecimal max(BigDecimal left, BigDecimal right) {
		if (left == null) {
			return right == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : right;
		}
		if (right == null) {
			return left;
		}
		return left.max(right);
	}

	private String formatMonthLabel(YearMonth month) {
		String label = month.format(MONTH_LABEL_FORMATTER);
		return label.substring(0, 1).toUpperCase(LOCALE_PT_BR) + label.substring(1);
	}
}
