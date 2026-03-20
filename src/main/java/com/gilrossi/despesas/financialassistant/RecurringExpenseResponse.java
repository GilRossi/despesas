package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseResponse(
	String description,
	String categoryName,
	String subcategoryName,
	BigDecimal averageAmount,
	int occurrences,
	boolean likelyFixedAmount,
	LocalDate lastOccurrence
) {
}
