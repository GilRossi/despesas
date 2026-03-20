package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationResult;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationRequest;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantGatewayException;

@Service
public class FinancialAssistantQueryService {

	private static final Logger log = LoggerFactory.getLogger(FinancialAssistantQueryService.class);

	private final FinancialAssistantIntentResolver intentResolver;
	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantInsightsService insightsService;
	private final FinancialAssistantRecommendationService recommendationService;
	private final FinancialAssistantConversationGateway conversationGateway;

	public FinancialAssistantQueryService(
		FinancialAssistantIntentResolver intentResolver,
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantInsightsService insightsService,
		FinancialAssistantRecommendationService recommendationService,
		FinancialAssistantConversationGateway conversationGateway
	) {
		this.intentResolver = intentResolver;
		this.analyticsService = analyticsService;
		this.insightsService = insightsService;
		this.recommendationService = recommendationService;
		this.conversationGateway = conversationGateway;
	}

	@Transactional(readOnly = true)
	public FinancialAssistantQueryResponse ask(FinancialAssistantQueryRequest request) {
		YearMonth referenceMonth = FinancialAssistantSupport.resolveReferenceMonth(request.referenceMonth());
		ResolvedFinancialAssistantQuery resolved = intentResolver.resolve(request.question(), referenceMonth);
		FinancialAssistantPeriodSummaryResponse summary = analyticsService.summarize(resolved.referenceMonth().atDay(1), resolved.referenceMonth().atEndOfMonth());
		FinancialAssistantInsightsResponse insights = insightsService.insights(resolved.referenceMonth());
		FinancialAssistantRecommendationsResponse recommendations = recommendationService.recommendations(resolved.referenceMonth());
		CategorySpendingResponse highestCategory = summary.categoryTotals().isEmpty() ? null : summary.categoryTotals().getFirst();
		List<TopExpenseResponse> topExpenses = analyticsService.topExpenses(resolved.referenceMonth(), 5);
		String answer = fallbackAnswer(resolved, summary, highestCategory, insights, recommendations.recommendations(), topExpenses);
		FinancialAssistantQueryMode mode = FinancialAssistantQueryMode.FALLBACK;
		FinancialAssistantAiUsage aiUsage = null;

		if (conversationGateway.isAvailable() && shouldUseAi(resolved, summary)) {
			try {
				FinancialAssistantConversationResult conversationResult = conversationGateway.answer(new FinancialAssistantConversationRequest(
					request.question(),
					resolved.referenceMonth().toString(),
					resolved.intent().name(),
					resolved.categoryName() == null ? "" : resolved.categoryName()
				));
				answer = conversationResult.answer();
				aiUsage = conversationResult.usage();
				mode = FinancialAssistantQueryMode.AI;
			} catch (FinancialAssistantGatewayException exception) {
				Throwable rootCause = FinancialAssistantAiFailureClassifier.rootCause(exception);
				log.warn(
					"Financial assistant AI fallback activated category={} intent={} month={} exceptionClass={}",
					exception.category(),
					resolved.intent(),
					resolved.referenceMonth(),
					rootCause.getClass().getSimpleName()
				);
				mode = FinancialAssistantQueryMode.FALLBACK;
			}
		}

		return new FinancialAssistantQueryResponse(
			request.question(),
			mode,
			resolved.intent(),
			answer,
			summary,
			insights.monthComparison(),
			highestCategory,
			topExpenses,
			insights.increaseAlerts(),
			insights.recurringExpenses(),
			recommendations.recommendations(),
			aiUsage
		);
	}

	private boolean shouldUseAi(
		ResolvedFinancialAssistantQuery resolved,
		FinancialAssistantPeriodSummaryResponse summary
	) {
		if (summary.totalExpenses() == 0) {
			return false;
		}

		return switch (resolved.intent()) {
			case MONTH_OVER_MONTH_CHANGE, SAVINGS_RECOMMENDATIONS, RECURRING_EXPENSES, INCREASE_ALERTS -> true;
			case TOTAL_BY_CATEGORY_IN_PERIOD, TOP_EXPENSES_IN_PERIOD, HIGHEST_SPENDING_CATEGORY, PERIOD_SUMMARY, UNKNOWN -> false;
		};
	}

	private String fallbackAnswer(
		ResolvedFinancialAssistantQuery resolved,
		FinancialAssistantPeriodSummaryResponse summary,
		CategorySpendingResponse highestCategory,
		FinancialAssistantInsightsResponse insights,
		List<RecommendationResponse> recommendations,
		List<TopExpenseResponse> topExpenses
	) {
		return switch (resolved.intent()) {
			case TOTAL_BY_CATEGORY_IN_PERIOD -> categoryTotalAnswer(resolved.referenceMonth(), resolved.categoryName());
			case TOP_EXPENSES_IN_PERIOD -> topExpensesAnswer(resolved.referenceMonth(), topExpenses);
			case MONTH_OVER_MONTH_CHANGE -> monthComparisonAnswer(insights.monthComparison());
			case SAVINGS_RECOMMENDATIONS -> recommendationsAnswer(recommendations);
			case RECURRING_EXPENSES -> recurringExpensesAnswer(insights.recurringExpenses());
			case HIGHEST_SPENDING_CATEGORY -> highestCategoryAnswer(resolved.referenceMonth(), highestCategory);
			case INCREASE_ALERTS -> increaseAlertsAnswer(insights.increaseAlerts());
			case PERIOD_SUMMARY -> periodSummaryAnswer(resolved.referenceMonth(), summary, highestCategory);
			case UNKNOWN -> "Nao consegui identificar uma intencao financeira especifica. Posso ajudar com resumo do mes, maiores gastos, comparacao entre meses, recorrencias ou economia.";
		};
	}

	private String categoryTotalAnswer(YearMonth referenceMonth, String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return "Nao identifiquei a categoria na pergunta. Informe a categoria desejada para eu calcular o total do periodo.";
		}
		BigDecimal total = analyticsService.totalByCategory(referenceMonth, categoryName);
		return "No periodo %s, o total gasto em %s foi %s.".formatted(referenceMonth, categoryName, total);
	}

	private String topExpensesAnswer(YearMonth referenceMonth, List<TopExpenseResponse> topExpenses) {
		if (topExpenses.isEmpty()) {
			return "Nao ha despesas registradas em %s para listar os maiores gastos.".formatted(referenceMonth);
		}
		TopExpenseResponse first = topExpenses.getFirst();
		return "Em %s, o maior gasto foi %s (%s). Consulte a lista detalhada para os demais lancamentos relevantes.".formatted(referenceMonth, first.description(), first.amount());
	}

	private String monthComparisonAnswer(MonthComparisonResponse comparison) {
		return "Em %s, o total foi %s contra %s em %s, com variacao de %s (%s%%).".formatted(
			comparison.currentMonth(),
			comparison.currentTotal(),
			comparison.previousTotal(),
			comparison.previousMonth(),
			comparison.deltaAmount(),
			comparison.deltaPercentage()
		);
	}

	private String recommendationsAnswer(List<RecommendationResponse> recommendations) {
		if (recommendations.isEmpty()) {
			return "Nao ha recomendacoes especificas para este periodo.";
		}
		RecommendationResponse first = recommendations.getFirst();
		return "%s: %s".formatted(first.title(), first.action());
	}

	private String recurringExpensesAnswer(List<RecurringExpenseResponse> recurringExpenses) {
		if (recurringExpenses.isEmpty()) {
			return "Nao encontrei despesas com sinal forte de recorrencia no historico analisado.";
		}
		RecurringExpenseResponse first = recurringExpenses.getFirst();
		return "A despesa %s aparece de forma recorrente em %s meses analisados, com media de %s.".formatted(
			first.description(),
			first.occurrences(),
			first.averageAmount()
		);
	}

	private String highestCategoryAnswer(YearMonth referenceMonth, CategorySpendingResponse highestCategory) {
		if (highestCategory == null) {
			return "Nao ha gastos registrados em %s para identificar a categoria dominante.".formatted(referenceMonth);
		}
		return "Em %s, a categoria com maior gasto foi %s, totalizando %s (%s%% do periodo).".formatted(
			referenceMonth,
			highestCategory.categoryName(),
			highestCategory.totalAmount(),
			highestCategory.sharePercentage()
		);
	}

	private String increaseAlertsAnswer(List<IncreaseAlertResponse> alerts) {
		if (alerts.isEmpty()) {
			return "Nao encontrei aumentos relevantes por categoria no comparativo com o mes anterior.";
		}
		IncreaseAlertResponse first = alerts.getFirst();
		return "%s aumentou %s em relacao ao mes anterior, um crescimento de %s%%.".formatted(
			first.categoryName(),
			first.deltaAmount(),
			first.deltaPercentage()
		);
	}

	private String periodSummaryAnswer(YearMonth referenceMonth, FinancialAssistantPeriodSummaryResponse summary, CategorySpendingResponse highestCategory) {
		if (summary.totalExpenses() == 0) {
			return "Nao ha despesas registradas em %s.".formatted(referenceMonth);
		}
		return "Em %s, voce registrou %s despesas somando %s. A categoria com maior peso foi %s.".formatted(
			referenceMonth,
			summary.totalExpenses(),
			summary.totalAmount(),
			highestCategory == null ? "indefinida" : highestCategory.categoryName()
		);
	}
}
