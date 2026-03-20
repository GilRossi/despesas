package com.gilrossi.despesas.financialassistant.ai;

import java.time.YearMonth;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantInsightsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantSupport;
import com.gilrossi.despesas.financialassistant.IncreaseAlertResponse;
import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;
import com.gilrossi.despesas.financialassistant.RecurringExpenseResponse;
import com.gilrossi.despesas.financialassistant.TopExpenseResponse;

import dev.langchain4j.agent.tool.Tool;

@Component
public class LangChain4jFinancialAssistantTools {

	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantInsightsService insightsService;
	private final FinancialAssistantRecommendationService recommendationService;

	public LangChain4jFinancialAssistantTools(
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantInsightsService insightsService,
		FinancialAssistantRecommendationService recommendationService
	) {
		this.analyticsService = analyticsService;
		this.insightsService = insightsService;
		this.recommendationService = recommendationService;
	}

	@Tool("Resume os gastos do mês informado no formato yyyy-MM. Se estiver vazio, usa o mês atual.")
	public String periodSummary(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		FinancialAssistantPeriodSummaryResponse summary = analyticsService.summarize(month.atDay(1), month.atEndOfMonth());
		return "mes=%s; total=%s; pagas=%s; pendentes=%s; categoria_lider=%s; categorias=%s; maiores=%s".formatted(
			month,
			summary.totalAmount(),
			summary.paidAmount(),
			summary.remainingAmount(),
			summary.highestSpendingCategory(),
			formatCategoryTotals(summary.categoryTotals(), 3),
			formatTopExpenses(summary.topExpenses(), 3)
		);
	}

	@Tool("Retorna o total de uma categoria no mês informado no formato yyyy-MM. Se o mês estiver vazio, usa o mês atual.")
	public String totalByCategory(String categoryName, String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		return "mes=%s; categoria=%s; total=%s".formatted(
			month,
			categoryName,
			analyticsService.totalByCategory(month, categoryName).toPlainString()
		);
	}

	@Tool("Retorna a categoria com maior gasto no mês informado no formato yyyy-MM.")
	public String highestSpendingCategory(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		CategorySpendingResponse category = analyticsService.highestSpendingCategory(month);
		if (category == null) {
			return "mes=%s; categoria_lider=nenhuma".formatted(month);
		}
		return "mes=%s; categoria=%s; total=%s; participacao=%s".formatted(
			month,
			category.categoryName(),
			category.totalAmount(),
			category.sharePercentage()
		);
	}

	@Tool("Lista as maiores despesas do mês informado no formato yyyy-MM.")
	public String topExpenses(String referenceMonth, int limit) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		int safeLimit = limit <= 0 ? 3 : Math.min(limit, 5);
		return "mes=%s; top=%s".formatted(month, formatTopExpenses(analyticsService.topExpenses(month, safeLimit), safeLimit));
	}

	@Tool("Compara o total do mês informado no formato yyyy-MM com o mês anterior.")
	public String monthOverMonthChange(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		MonthComparisonResponse comparison = analyticsService.compareMonths(month);
		return "mes_atual=%s; total_atual=%s; mes_anterior=%s; total_anterior=%s; delta=%s; variacao_percentual=%s".formatted(
			comparison.currentMonth(),
			comparison.currentTotal(),
			comparison.previousMonth(),
			comparison.previousTotal(),
			comparison.deltaAmount(),
			comparison.deltaPercentage()
		);
	}

	@Tool("Lista alertas de aumento relevante por categoria no mês informado no formato yyyy-MM.")
	public String increaseAlerts(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		return "mes=%s; alertas=%s".formatted(month, formatIncreaseAlerts(analyticsService.increaseAlerts(month), 3));
	}

	@Tool("Lista despesas recorrentes detectadas considerando o mês informado no formato yyyy-MM.")
	public String recurringExpenses(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		return "mes=%s; recorrencias=%s".formatted(month, formatRecurringExpenses(analyticsService.recurringExpenses(month), 5));
	}

	@Tool("Retorna recomendações iniciais de economia considerando o mês informado no formato yyyy-MM.")
	public String recommendations(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		return "mes=%s; recomendacoes=%s".formatted(month, formatRecommendations(recommendationService.recommendations(month).recommendations(), 4));
	}

	@Tool("Retorna insights combinando comparação mensal, aumentos relevantes e recorrências.")
	public String insights(String referenceMonth) {
		YearMonth month = FinancialAssistantSupport.resolveReferenceMonth(referenceMonth);
		MonthComparisonResponse comparison = analyticsService.compareMonths(month);
		return "comparativo={mes_atual=%s; total_atual=%s; mes_anterior=%s; total_anterior=%s; delta=%s; variacao_percentual=%s}; aumentos=%s; recorrencias=%s".formatted(
			comparison.currentMonth(),
			comparison.currentTotal(),
			comparison.previousMonth(),
			comparison.previousTotal(),
			comparison.deltaAmount(),
			comparison.deltaPercentage(),
			formatIncreaseAlerts(analyticsService.increaseAlerts(month), 3),
			formatRecurringExpenses(analyticsService.recurringExpenses(month), 5)
		);
	}

	private String formatCategoryTotals(List<CategorySpendingResponse> totals, int limit) {
		return join(totals.stream()
			.limit(limit)
			.map(item -> "%s:%s(%s%%)".formatted(item.categoryName(), item.totalAmount(), item.sharePercentage()))
			.toList());
	}

	private String formatTopExpenses(List<TopExpenseResponse> expenses, int limit) {
		return join(expenses.stream()
			.limit(limit)
			.map(item -> "%s|%s|%s|%s".formatted(item.description(), item.amount(), item.categoryName(), item.dueDate()))
			.toList());
	}

	private String formatIncreaseAlerts(List<IncreaseAlertResponse> alerts, int limit) {
		return join(alerts.stream()
			.limit(limit)
			.map(item -> "%s:+%s(%s%%)".formatted(item.categoryName(), item.deltaAmount(), item.deltaPercentage()))
			.toList());
	}

	private String formatRecurringExpenses(List<RecurringExpenseResponse> recurringExpenses, int limit) {
		return join(recurringExpenses.stream()
			.limit(limit)
			.map(item -> "%s|media=%s|ocorrencias=%s|fixa=%s".formatted(
				item.description(),
				item.averageAmount(),
				item.occurrences(),
				item.likelyFixedAmount()
			))
			.toList());
	}

	private String formatRecommendations(List<RecommendationResponse> recommendations, int limit) {
		return join(recommendations.stream()
			.limit(limit)
			.map(item -> "%s:%s".formatted(item.title(), item.action()))
			.toList());
	}

	private String join(List<String> items) {
		StringJoiner joiner = new StringJoiner("; ", "[", "]");
		items.forEach(joiner::add);
		return joiner.toString();
	}
}
