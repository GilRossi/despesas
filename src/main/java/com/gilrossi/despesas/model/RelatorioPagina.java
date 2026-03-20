package com.gilrossi.despesas.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import com.gilrossi.despesas.financialassistant.IncreaseAlertResponse;
import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;
import com.gilrossi.despesas.financialassistant.RecurringExpenseResponse;
import com.gilrossi.despesas.financialassistant.TopExpenseResponse;

public record RelatorioPagina(
	YearMonth referenceMonth,
	String monthInputValue,
	String periodLabel,
	boolean compareWithPrevious,
	long totalExpenses,
	BigDecimal totalAmount,
	BigDecimal paidAmount,
	BigDecimal remainingAmount,
	String highestCategoryName,
	BigDecimal highestCategoryAmount,
	BigDecimal highestCategoryShare,
	MonthComparisonResponse monthComparison,
	int currentComparisonWidth,
	int previousComparisonWidth,
	int paidShareWidth,
	int remainingShareWidth,
	List<RelatorioCategoriaLinha> categoryBreakdown,
	List<TopExpenseResponse> topExpenses,
	List<RecurringExpenseResponse> recurringExpenses,
	List<IncreaseAlertResponse> increaseAlerts,
	List<RecommendationResponse> recommendations,
	List<RelatorioAssistenteAcao> assistantActions
) {

	public boolean hasData() {
		return totalExpenses > 0;
	}

	public boolean showComparison() {
		return compareWithPrevious && monthComparison != null;
	}
}
