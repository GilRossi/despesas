package com.gilrossi.despesas.model;

import java.math.BigDecimal;

public record RelatorioCategoriaLinha(
	String categoryName,
	BigDecimal totalAmount,
	BigDecimal sharePercentage,
	BigDecimal previousAmount,
	BigDecimal deltaAmount,
	BigDecimal deltaPercentage,
	int barWidth
) {

	public boolean increased() {
		return deltaAmount != null && deltaAmount.compareTo(BigDecimal.ZERO) > 0;
	}

	public boolean decreased() {
		return deltaAmount != null && deltaAmount.compareTo(BigDecimal.ZERO) < 0;
	}

	public boolean stable() {
		return deltaAmount == null || deltaAmount.compareTo(BigDecimal.ZERO) == 0;
	}

	public boolean newInCurrentMonth() {
		return previousAmount != null
			&& previousAmount.compareTo(BigDecimal.ZERO) == 0
			&& totalAmount != null
			&& totalAmount.compareTo(BigDecimal.ZERO) > 0;
	}
}
