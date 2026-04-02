package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialAssistantRecommendationService {

	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantInsightsService insightsService;
	private final FinancialAssistantAccessContextProvider accessContextProvider;

	public FinancialAssistantRecommendationService(
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantInsightsService insightsService,
		FinancialAssistantAccessContextProvider accessContextProvider
	) {
		this.analyticsService = analyticsService;
		this.insightsService = insightsService;
		this.accessContextProvider = accessContextProvider;
	}

	@Transactional(readOnly = true)
	public FinancialAssistantRecommendationsResponse recommendations(YearMonth referenceMonth) {
		return recommendations(accessContextProvider.requireContext(), referenceMonth);
	}

	@Transactional(readOnly = true)
	FinancialAssistantRecommendationsResponse recommendations(FinancialAssistantAccessContext context, YearMonth referenceMonth) {
		FinancialAssistantPeriodSummaryResponse summary = analyticsService.summarize(context, referenceMonth.atDay(1), referenceMonth.atEndOfMonth());
		FinancialAssistantInsightsResponse insights = insightsService.insights(context, referenceMonth);
		List<RecommendationResponse> recommendations = new ArrayList<>();

		if (summary.totalExpenses() == 0) {
			recommendations.add(new RecommendationResponse(
				"Sem dados suficientes",
				"Ainda não há despesas registradas neste período para gerar sugestões úteis.",
				"Registre despesas do seu espaço para liberar comparações e próximos passos."
			));
			return new FinancialAssistantRecommendationsResponse(recommendations);
		}

		CategorySpendingResponse highestCategory = summary.categoryTotals().isEmpty() ? null : summary.categoryTotals().getFirst();
		if (highestCategory != null && highestCategory.sharePercentage().compareTo(new BigDecimal("35.00")) >= 0) {
			recommendations.add(new RecommendationResponse(
				"Revise a categoria que mais pesa",
				"A categoria %s representa %s%% do total do período.".formatted(highestCategory.categoryName(), highestCategory.sharePercentage()),
				"Analise contratos, frequência e alternativas para reduzir o peso dessa categoria."
			));
		}

		if (!insights.increaseAlerts().isEmpty()) {
			IncreaseAlertResponse alert = insights.increaseAlerts().getFirst();
			recommendations.add(new RecommendationResponse(
				"Investigue o aumento mais relevante",
				"%s subiu %s em relação ao mês anterior.".formatted(alert.categoryName(), alert.deltaAmount()),
				"Verifique o que mudou nessa categoria e se o aumento foi pontual ou tende a se repetir."
			));
		}

		List<RecurringExpenseResponse> fixedRecurring = insights.recurringExpenses().stream()
			.filter(RecurringExpenseResponse::likelyFixedAmount)
			.toList();
		if (!fixedRecurring.isEmpty()) {
			recommendations.add(new RecommendationResponse(
				"Renegocie despesas recorrentes",
				"Existem %s despesas com comportamento recorrente e valor relativamente estável.".formatted(fixedRecurring.size()),
				"Priorize revisão de contratos e assinaturas para liberar caixa mensal."
			));
		}

		if (!summary.topExpenses().isEmpty()) {
			TopExpenseResponse topExpense = summary.topExpenses().getFirst();
			BigDecimal topShare = FinancialAssistantSupport.percentage(topExpense.amount(), summary.totalAmount());
			if (topShare.compareTo(new BigDecimal("25.00")) >= 0) {
				recommendations.add(new RecommendationResponse(
					"Monitore o maior gasto individual",
					"A maior despesa do período representa %s%% do total.".formatted(topShare),
					"Confirme se esse gasto era planejado e se precisa de provisão ou parcelamento melhor controlado."
				));
			}
		}

		if (recommendations.isEmpty()) {
			recommendations.add(new RecommendationResponse(
				"Padrão financeiro estável",
				"Não houve sinais fortes de concentração ou aumento atípico no período.",
				"Mantenha o acompanhamento mensal e valide variações por categoria ao fechar o próximo mês."
			));
		}

		return new FinancialAssistantRecommendationsResponse(recommendations);
	}
}
