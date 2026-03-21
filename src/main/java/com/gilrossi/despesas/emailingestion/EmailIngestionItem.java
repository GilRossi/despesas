package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;

public record EmailIngestionItem(
	String description,
	BigDecimal amount,
	BigDecimal quantity
) {
}
