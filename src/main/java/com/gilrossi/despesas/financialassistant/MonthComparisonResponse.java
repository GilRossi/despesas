package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;

public record MonthComparisonResponse(
	String currentMonth,
	BigDecimal currentTotal,
	String previousMonth,
	BigDecimal previousTotal,
	BigDecimal deltaAmount,
	BigDecimal deltaPercentage
) {
}
