package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class FinancialAssistantAnalyticsServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private FinancialAssistantAccessContextProvider accessContextProvider;

	private FinancialAssistantAnalyticsService service;

	@BeforeEach
	void setUp() {
		service = new FinancialAssistantAnalyticsService(expenseRepository, paymentRepository, accessContextProvider);
		when(accessContextProvider.requireContext()).thenReturn(new FinancialAssistantAccessContext(1L, 7L, "OWNER"));
	}

	@Test
	void deve_resumir_periodo_com_analise_por_categoria_e_top_despesas() {
		LocalDate from = LocalDate.of(2026, 3, 1);
		LocalDate to = LocalDate.of(2026, 3, 31);
		Expense alimentacao1 = expense(1L, "Mercado", "200.00", LocalDate.of(2026, 3, 5), 10L, "Alimentação", "Supermercado");
		Expense transporte = expense(2L, "Combustível", "100.00", LocalDate.of(2026, 3, 6), 11L, "Transporte", "Combustível");
		Expense alimentacao2 = expense(3L, "Restaurante", "50.00", LocalDate.of(2026, 3, 7), 10L, "Alimentação", "Restaurante");
		when(expenseRepository.findAllByHouseholdIdAndDueDateBetween(7L, from, to)).thenReturn(List.of(alimentacao1, transporte, alimentacao2));
		when(paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(List.of(1L, 2L, 3L))).thenReturn(List.of(
			payment(1L, 1L, "40.00")
		));

		FinancialAssistantPeriodSummaryResponse response = service.summarize(from, to);

		assertEquals(3, response.totalExpenses());
		assertEquals(new BigDecimal("350.00"), response.totalAmount());
		assertEquals(new BigDecimal("40.00"), response.paidAmount());
		assertEquals("Alimentação", response.highestSpendingCategory());
		assertEquals(2, response.categoryTotals().size());
		assertEquals("Alimentação", response.categoryTotals().getFirst().categoryName());
		assertEquals(new BigDecimal("250.00"), response.categoryTotals().getFirst().totalAmount());
		assertEquals("Mercado", response.topExpenses().getFirst().description());
		assertEquals(new BigDecimal("250.00"), service.totalByCategory(YearMonth.of(2026, 3), "alimentacao"));
	}

	@Test
	void deve_comparar_mes_atual_com_anterior() {
		YearMonth referenceMonth = YearMonth.of(2026, 3);
		when(expenseRepository.findAllByHouseholdIdAndDueDateBetween(7L, referenceMonth.atDay(1), referenceMonth.atEndOfMonth()))
			.thenReturn(List.of(expense(1L, "Mercado", "300.00", LocalDate.of(2026, 3, 5), 10L, "Alimentação", "Supermercado")));
		when(expenseRepository.findAllByHouseholdIdAndDueDateBetween(7L, referenceMonth.minusMonths(1).atDay(1), referenceMonth.minusMonths(1).atEndOfMonth()))
			.thenReturn(List.of(expense(2L, "Mercado", "200.00", LocalDate.of(2026, 2, 5), 10L, "Alimentação", "Supermercado")));
		when(paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(List.of(1L))).thenReturn(List.of());
		when(paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(List.of(2L))).thenReturn(List.of());

		MonthComparisonResponse response = service.compareMonths(referenceMonth);

		assertEquals("2026-03", response.currentMonth());
		assertEquals(new BigDecimal("300.00"), response.currentTotal());
		assertEquals(new BigDecimal("200.00"), response.previousTotal());
		assertEquals(new BigDecimal("100.00"), response.deltaAmount());
		assertEquals(new BigDecimal("50.00"), response.deltaPercentage());
	}

	@Test
	void deve_detectar_despesas_recorrentes() {
		YearMonth referenceMonth = YearMonth.of(2026, 3);
		when(expenseRepository.findAllByHouseholdIdAndDueDateGreaterThanEqual(7L, LocalDate.of(2025, 10, 1))).thenReturn(List.of(
			expense(1L, "Academia", "120.00", LocalDate.of(2026, 1, 10), 20L, "Saúde", "Academia"),
			expense(2L, "Academia", "120.00", LocalDate.of(2026, 2, 10), 20L, "Saúde", "Academia"),
			expense(3L, "Academia", "118.00", LocalDate.of(2026, 3, 10), 20L, "Saúde", "Academia"),
			expense(4L, "Viagem", "500.00", LocalDate.of(2026, 3, 20), 30L, "Lazer", "Passeio")
		));

		List<RecurringExpenseResponse> response = service.recurringExpenses(referenceMonth);

		assertEquals(1, response.size());
		assertEquals("Academia", response.getFirst().description());
		assertEquals(3, response.getFirst().occurrences());
		assertTrue(response.getFirst().likelyFixedAmount());
	}

	@Test
	void deve_retornar_kpis_zerados_sem_dados() {
		LocalDate from = LocalDate.of(2026, 4, 1);
		LocalDate to = LocalDate.of(2026, 4, 30);
		when(expenseRepository.findAllByHouseholdIdAndDueDateBetween(7L, from, to)).thenReturn(List.of());

		FinancialAssistantKpisResponse response = service.kpis(from, to);

		assertEquals(6, response.kpis().size());
		assertEquals(new BigDecimal("0.00"), response.kpis().getFirst().amount());
	}

	private Expense expense(Long id, String description, String amount, LocalDate dueDate, Long categoryId, String categoryName, String subcategoryName) {
		Expense expense = new Expense(
			7L,
			description,
			new BigDecimal(amount),
			dueDate,
			ExpenseContext.CASA,
			categoryId,
			categoryName,
			categoryId + 100,
			subcategoryName,
			null
		);
		expense.setId(id);
		return expense;
	}

	private Payment payment(Long id, Long expenseId, String amount) {
		Payment payment = new Payment(expenseId, new BigDecimal(amount), LocalDate.of(2026, 3, 8), PaymentMethod.PIX, null);
		payment.setId(id);
		return payment;
	}
}
