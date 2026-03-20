package com.gilrossi.despesas.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ExpenseStatusCalculatorTest {

	private final ExpenseStatusCalculator calculator = new ExpenseStatusCalculator();

	@Test
	void deve_marcar_prevista_quando_vencimento_for_no_futuro_e_sem_pagamentos() {
		ExpenseStatus status = calculator.calcular(
			new BigDecimal("100.00"),
			BigDecimal.ZERO,
			LocalDate.now().plusDays(2),
			LocalDate.now()
		);

		assertEquals(ExpenseStatus.PREVISTA, status);
	}

	@Test
	void deve_marcar_aberta_quando_vencimento_for_hoje_e_sem_pagamentos() {
		ExpenseStatus status = calculator.calcular(
			new BigDecimal("100.00"),
			BigDecimal.ZERO,
			LocalDate.now(),
			LocalDate.now()
		);

		assertEquals(ExpenseStatus.ABERTA, status);
	}

	@Test
	void deve_marcar_vencida_quando_vencimento_ja_passou_e_sem_pagamentos() {
		ExpenseStatus status = calculator.calcular(
			new BigDecimal("100.00"),
			BigDecimal.ZERO,
			LocalDate.now().minusDays(1),
			LocalDate.now()
		);

		assertEquals(ExpenseStatus.VENCIDA, status);
	}

	@Test
	void deve_marcar_parcialmente_paga_quando_parte_do_valor_foi_paga() {
		ExpenseStatus status = calculator.calcular(
			new BigDecimal("100.00"),
			new BigDecimal("30.00"),
			LocalDate.now().minusDays(1),
			LocalDate.now()
		);

		assertEquals(ExpenseStatus.PARCIALMENTE_PAGA, status);
	}

	@Test
	void deve_marcar_paga_quando_valor_total_foi_pago() {
		ExpenseStatus status = calculator.calcular(
			new BigDecimal("100.00"),
			new BigDecimal("100.00"),
			LocalDate.now().minusDays(1),
			LocalDate.now()
		);

		assertEquals(ExpenseStatus.PAGA, status);
	}
}
