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

	public FinancialAssistantRecommendationService(FinancialAssistantAnalyticsService analyticsService, FinancialAssistantInsightsService insightsService) {
		this.analyticsService = analyticsService;
		this.insightsService = insightsService;
	}

	@Transactional(readOnly = true)
	public FinancialAssistantRecommendationsResponse recommendations(YearMonth referenceMonth) {
		FinancialAssistantPeriodSummaryResponse summary = analyticsService.summarize(referenceMonth.atDay(1), referenceMonth.atEndOfMonth());
		FinancialAssistantInsightsResponse insights = insightsService.insights(referenceMonth);
		List<RecommendationResponse> recommendations = new ArrayList<>();

		if (summary.totalExpenses() == 0) {
			recommendations.add(new RecommendationResponse(
				"Sem dados suficientes",
				"Nao ha despesas registradas no periodo para gerar recomendacoes robustas.",
				"Registre despesas do household para habilitar insights e comparacoes."
			));
			return new FinancialAssistantRecommendationsResponse(recommendations);
		}

		CategorySpendingResponse highestCategory = summary.categoryTotals().isEmpty() ? null : summary.categoryTotals().getFirst();
		if (highestCategory != null && highestCategory.sharePercentage().compareTo(new BigDecimal("35.00")) >= 0) {
			recommendations.add(new RecommendationResponse(
				"Revise a categoria que mais pesa",
				"A categoria %s representa %s%% do total do periodo.".formatted(highestCategory.categoryName(), highestCategory.sharePercentage()),
				"Analise contratos, frequencia e alternativas para reduzir o peso dessa categoria."
			));
		}

		if (!insights.increaseAlerts().isEmpty()) {
			IncreaseAlertResponse alert = insights.increaseAlerts().getFirst();
			recommendations.add(new RecommendationResponse(
				"Investigue o aumento mais relevante",
				"%s subiu %s em relacao ao mes anterior.".formatted(alert.categoryName(), alert.deltaAmount()),
				"Verifique o que mudou nessa categoria e se o aumento foi pontual ou tende a se repetir."
			));
		}

		List<RecurringExpenseResponse> fixedRecurring = insights.recurringExpenses().stream()
			.filter(RecurringExpenseResponse::likelyFixedAmount)
			.toList();
		if (!fixedRecurring.isEmpty()) {
			recommendations.add(new RecommendationResponse(
				"Renegocie despesas recorrentes",
				"Existem %s despesas com comportamento recorrente e valor relativamente estavel.".formatted(fixedRecurring.size()),
				"Priorize revisao de contratos e assinaturas para liberar caixa mensal."
			));
		}

		if (!summary.topExpenses().isEmpty()) {
			TopExpenseResponse topExpense = summary.topExpenses().getFirst();
			BigDecimal topShare = FinancialAssistantSupport.percentage(topExpense.amount(), summary.totalAmount());
			if (topShare.compareTo(new BigDecimal("25.00")) >= 0) {
				recommendations.add(new RecommendationResponse(
					"Monitore o maior gasto individual",
					"A maior despesa do periodo representa %s%% do total.".formatted(topShare),
					"Confirme se esse gasto era planejado e se precisa de provisao ou parcelamento melhor controlado."
				));
			}
		}

		if (recommendations.isEmpty()) {
			recommendations.add(new RecommendationResponse(
				"Padrao financeiro estavel",
				"Nao houve sinais fortes de concentracao ou aumento atipico no periodo.",
				"Mantenha o acompanhamento mensal e valide variacoes por categoria ao fechar o proximo mes."
			));
		}

		return new FinancialAssistantRecommendationsResponse(recommendations);
	}
}
