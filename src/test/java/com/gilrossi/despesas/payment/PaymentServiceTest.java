package com.gilrossi.despesas.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseNotFoundException;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.ExpenseStatusCalculator;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private PaymentService service;

	@BeforeEach
	void setUp() {
		service = new PaymentService(expenseRepository, paymentRepository, currentHouseholdProvider, new ExpenseStatusCalculator());
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_registrar_pagamento_parcial_na_unidade_correta() {
		Expense expense = novaExpense(1L, "Internet", "100.00", LocalDate.now().minusDays(1));
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(1L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseId(1L)).thenReturn(List.of());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(10L);
			return payment;
		});

		PaymentResponse response = service.registrar(
			new CreatePaymentRequest(1L, new BigDecimal("40.00"), LocalDate.now(), PaymentMethod.PIX, "Primeira parcela")
		);

		assertEquals(new BigDecimal("40.00"), response.amount());
		assertEquals(ExpenseStatus.PARCIALMENTE_PAGA, response.expenseStatus());
		assertEquals(new BigDecimal("60.00"), response.expenseRemainingAmount());
	}

	@Test
	void deve_registrar_quitacao_total_e_marcar_despesa_como_paga() {
		Expense expense = novaExpense(4L, "Internet", "100.00", LocalDate.now().minusDays(1));
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(4L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseId(4L)).thenReturn(List.of(novaPayment(1L, 4L, "40.00")));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			payment.setId(12L);
			return payment;
		});

		PaymentResponse response = service.registrar(
			new CreatePaymentRequest(4L, new BigDecimal("60.00"), LocalDate.now(), PaymentMethod.PIX, "Quitacao final")
		);

		assertEquals(new BigDecimal("60.00"), response.amount());
		assertEquals(ExpenseStatus.PAGA, response.expenseStatus());
		assertEquals(new BigDecimal("0.00"), response.expenseRemainingAmount());
		assertEquals(new BigDecimal("100.00"), response.expensePaidAmount());
	}

	@Test
	void deve_listar_pagamentos_da_despesa_na_ordem_esperada() {
		Expense expense = novaExpense(2L, "Internet", "100.00", LocalDate.now().minusDays(1));
		when(expenseRepository.findByIdAndHouseholdId(2L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseIdOrderByPaidAtDescIdDesc(eq(2L), any(PageRequest.class))).thenReturn(
			new PageImpl<>(
				List.of(
					novaPayment(11L, 2L, "30.00"),
					novaPayment(10L, 2L, "40.00")
				)
			)
		);

		var page = service.listarPorDespesa(2L, 0, 10);

		assertEquals(2, page.content().size());
		assertEquals(11L, page.content().getFirst().id());
		assertEquals(10L, page.content().get(1).id());
	}

	@Test
	void deve_rejeitar_pagamento_maior_que_o_saldo_restante() {
		Expense expense = novaExpense(3L, "Internet", "100.00", LocalDate.now().minusDays(1));
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(3L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseId(3L)).thenReturn(List.of(novaPayment(1L, 3L, "70.00")));

		assertThrows(PaymentBusinessRuleException.class, () ->
			service.registrar(new CreatePaymentRequest(3L, new BigDecimal("40.00"), LocalDate.now(), PaymentMethod.PIX, "Excesso"))
		);
	}

	@Test
	void deve_rejeitar_pagamento_quando_despesa_nao_existir() {
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(99L, 7L)).thenReturn(Optional.empty());

		assertThrows(ExpenseNotFoundException.class, () ->
			service.registrar(new CreatePaymentRequest(99L, new BigDecimal("10.00"), LocalDate.now(), PaymentMethod.PIX, "Invalido"))
		);
	}

	private Expense novaExpense(Long id, String descricao, String valor, LocalDate vencimento) {
		Expense expense = new Expense(
			descricao,
			new BigDecimal(valor),
			vencimento,
			ExpenseContext.CASA,
			10L,
			"Moradia",
			20L,
			"Internet",
			"Observacao"
		);
		expense.setId(id);
		return expense;
	}

	private Payment novaPayment(Long id, Long expenseId, String valor) {
		Payment payment = new Payment(
			expenseId,
			new BigDecimal(valor),
			LocalDate.now(),
			PaymentMethod.PIX,
			"Pagamento anterior"
		);
		payment.setId(id);
		return payment;
	}
}
