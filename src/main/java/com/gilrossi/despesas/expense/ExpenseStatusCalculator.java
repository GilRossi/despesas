package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class ExpenseStatusCalculator {

	public ExpenseStatus calcular(BigDecimal amount, BigDecimal paidAmount, LocalDate dueDate, LocalDate currentDate) {
		if (paidAmount.compareTo(amount) >= 0) {
			return ExpenseStatus.PAGA;
		}
		if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
			return ExpenseStatus.PARCIALMENTE_PAGA;
		}
		if (dueDate.isAfter(currentDate)) {
			return ExpenseStatus.PREVISTA;
		}
		if (dueDate.isBefore(currentDate)) {
			return ExpenseStatus.VENCIDA;
		}
		return ExpenseStatus.ABERTA;
	}
}
