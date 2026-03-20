package com.gilrossi.despesas.financialassistant;

import java.util.List;

public record FinancialAssistantInsightsResponse(
	MonthComparisonResponse monthComparison,
	List<IncreaseAlertResponse> increaseAlerts,
	List<RecurringExpenseResponse> recurringExpenses
) {
}
