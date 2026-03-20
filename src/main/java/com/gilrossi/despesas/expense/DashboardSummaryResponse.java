package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
	Long householdId,
	long totalExpenses,
	BigDecimal totalAmount,
	BigDecimal paidAmount,
	BigDecimal remainingAmount,
	long overdueCount,
	BigDecimal overdueAmount,
	long openCount,
	BigDecimal openAmount,
	List<DashboardStatusSummary> statuses
) {
}
