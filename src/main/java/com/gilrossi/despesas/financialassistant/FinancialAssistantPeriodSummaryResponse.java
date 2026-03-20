package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialAssistantPeriodSummaryResponse(
	LocalDate from,
	LocalDate to,
	long totalExpenses,
	BigDecimal totalAmount,
	BigDecimal paidAmount,
	BigDecimal remainingAmount,
	String highestSpendingCategory,
	List<CategorySpendingResponse> categoryTotals,
	List<TopExpenseResponse> topExpenses
) {
}
