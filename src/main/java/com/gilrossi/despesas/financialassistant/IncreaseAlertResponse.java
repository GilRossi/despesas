package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;

public record IncreaseAlertResponse(
	String categoryName,
	BigDecimal currentAmount,
	BigDecimal previousAmount,
	BigDecimal deltaAmount,
	BigDecimal deltaPercentage
) {
}
