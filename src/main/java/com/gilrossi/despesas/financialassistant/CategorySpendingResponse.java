package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;

public record CategorySpendingResponse(
	Long categoryId,
	String categoryName,
	BigDecimal totalAmount,
	long expensesCount,
	BigDecimal sharePercentage
) {
}
