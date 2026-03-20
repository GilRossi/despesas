package com.gilrossi.despesas.financialassistant;

import java.time.YearMonth;

public record ResolvedFinancialAssistantQuery(
	FinancialAssistantIntent intent,
	YearMonth referenceMonth,
	String categoryName
) {
}
