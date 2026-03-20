package com.gilrossi.despesas.expense;

import java.math.BigDecimal;

public record DashboardStatusSummary(ExpenseStatus status, long count, BigDecimal amount) {
}
