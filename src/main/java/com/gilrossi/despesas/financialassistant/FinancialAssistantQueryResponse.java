package com.gilrossi.despesas.financialassistant;

import java.util.List;

public record FinancialAssistantQueryResponse(
	String question,
	FinancialAssistantQueryMode mode,
	FinancialAssistantIntent intent,
	String answer,
	FinancialAssistantPeriodSummaryResponse summary,
	MonthComparisonResponse monthComparison,
	CategorySpendingResponse highestSpendingCategory,
	List<TopExpenseResponse> topExpenses,
	List<IncreaseAlertResponse> increaseAlerts,
	List<RecurringExpenseResponse> recurringExpenses,
	List<RecommendationResponse> recommendations,
	FinancialAssistantAiUsage aiUsage
) {
}
