package com.gilrossi.despesas.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private ExpenseStatusCalculator expenseStatusCalculator;
	private DashboardSummaryService service;

	@BeforeEach
	void setUp() {
		expenseStatusCalculator = new ExpenseStatusCalculator();
		service = new DashboardSummaryService(expenseRepository, paymentRepository, currentHouseholdProvider, expenseStatusCalculator);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_consolidar_resumo_financeiro_do_household() {
		Expense vencida = new Expense(7L, "Internet", new BigDecimal("120.00"), LocalDate.now().minusDays(2), ExpenseContext.CASA, 10L, "Casa", 100L, "Internet", null);
		vencida.setId(1L);
		Expense futura = new Expense(7L, "Mercado", new BigDecimal("200.00"), LocalDate.now().plusDays(3), ExpenseContext.CASA, 10L, "Casa", 101L, "Mercado", null);
		futura.setId(2L);
		Expense parcial = new Expense(7L, "Gasolina", new BigDecimal("150.00"), LocalDate.now().plusDays(1), ExpenseContext.VEICULO, 20L, "Transporte", 200L, "Combustivel", null);
		parcial.setId(3L);
		when(expenseRepository.findAllByHouseholdId(7L)).thenReturn(List.of(vencida, futura, parcial));
		when(paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(List.of(1L, 2L, 3L))).thenReturn(List.of(
			new Payment(3L, new BigDecimal("40.00"), LocalDate.now(), PaymentMethod.PIX, "Parcial")
		));

		DashboardSummaryResponse summary = service.resumir();

		assertThat(summary.householdId()).isEqualTo(7L);
		assertThat(summary.totalExpenses()).isEqualTo(3);
		assertThat(summary.totalAmount()).isEqualByComparingTo("470.00");
		assertThat(summary.paidAmount()).isEqualByComparingTo("40.00");
		assertThat(summary.remainingAmount()).isEqualByComparingTo("430.00");
		assertThat(summary.overdueCount()).isEqualTo(1);
		assertThat(summary.overdueAmount()).isEqualByComparingTo("120.00");
		assertThat(summary.openCount()).isEqualTo(2);
		assertThat(summary.openAmount()).isEqualByComparingTo("310.00");
		assertThat(summary.statuses()).extracting(DashboardStatusSummary::status)
			.containsExactlyInAnyOrder(ExpenseStatus.VENCIDA, ExpenseStatus.PREVISTA, ExpenseStatus.PARCIALMENTE_PAGA);
		assertThat(summary.statuses()).extracting(DashboardStatusSummary::amount)
			.containsExactlyInAnyOrder(
				new BigDecimal("120.00"),
				new BigDecimal("200.00"),
				new BigDecimal("110.00")
			);
	}
}
