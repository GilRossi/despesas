package com.gilrossi.despesas.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;
import com.gilrossi.despesas.spacereference.SpaceReferenceRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SubcategoryRepository subcategoryRepository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	@Mock
	private SpaceReferenceRepository spaceReferenceRepository;

	private ExpenseService service;

	@BeforeEach
	void setUp() {
		service = new ExpenseService(
			expenseRepository,
			paymentRepository,
			categoryRepository,
			subcategoryRepository,
			spaceReferenceRepository,
			currentHouseholdProvider,
			new ExpenseStatusCalculator()
		);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
	}

	@Test
	void deve_criar_despesa_usando_categoryId_e_subcategoryId_da_unidade() {
		Category categoria = new Category(10L, "Moradia", true);
		Subcategory subcategoria = new Subcategory(20L, 10L, "Internet", true);
		when(categoryRepository.findById(7L, 10L)).thenReturn(Optional.of(categoria));
		when(subcategoryRepository.findById(7L, 20L)).thenReturn(Optional.of(subcategoria));
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
			Expense expense = invocation.getArgument(0);
			expense.setId(1L);
			return expense;
		});
		when(paymentRepository.findByExpenseId(1L)).thenReturn(List.of());

		ExpenseResponse response = service.criar(new CreateExpenseRequest(
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.now(),
			LocalDate.now().plusDays(5),
			ExpenseContext.CASA,
			10L,
			20L,
			null,
			"Conta fixa"
		));

		assertEquals(1L, response.id());
		assertEquals(new ReferenceResponse(10L, "Moradia"), response.category());
		assertEquals(new ReferenceResponse(20L, "Internet"), response.subcategory());
		assertEquals(ExpenseStatus.PREVISTA, response.status());
		assertEquals(new BigDecimal("120.00"), response.remainingAmount());
	}

	@Test
	void deve_rejeitar_despesa_quando_subcategoria_nao_pertencer_a_categoria() {
		when(categoryRepository.findById(7L, 10L)).thenReturn(Optional.of(new Category(10L, "Moradia", true)));
		when(subcategoryRepository.findById(7L, 20L)).thenReturn(Optional.of(new Subcategory(20L, 11L, "Internet", true)));

		assertThrows(IllegalArgumentException.class, () -> service.criar(new CreateExpenseRequest(
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.now(),
			LocalDate.now().plusDays(5),
			ExpenseContext.CASA,
			10L,
			20L,
			null,
			"Conta fixa"
		)));
	}

	@Test
	void deve_listar_despesas_filtradas_por_contexto_e_categoria_id() {
		Expense casaAberta = novaExpense(1L, "Mercado", "100.00", LocalDate.now(), ExpenseContext.CASA, 10L, 20L);
		Page<Expense> page = new PageImpl<>(
			List.of(casaAberta),
			PageRequest.of(0, 10, Sort.by(Sort.Order.desc("dueDate"), Sort.Order.desc("id"))),
			1
		);

		when(expenseRepository.findAllByFilters(
			eq(7L),
			any(),
			any(ExpenseContext.class),
			any(Long.class),
			any(),
			any(ExpenseStatus.class),
			any(),
			any(),
			any(),
			any(),
			any(PageRequest.class)
		)).thenReturn(page);
		when(paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(List.of(1L))).thenReturn(List.of());

		PageResponse<ExpenseResponse> response = service.listar(
			new ExpenseFilter(null, ExpenseContext.CASA, 10L, null, ExpenseStatus.ABERTA, null, null, null, null),
			0,
			10
		);

		assertEquals(1, response.content().size());
		assertEquals(Long.valueOf(1L), response.content().getFirst().id());
		ArgumentCaptor<PageRequest> pageableCaptor = ArgumentCaptor.forClass(PageRequest.class);
		verify(expenseRepository).findAllByFilters(
			eq(7L),
			org.mockito.ArgumentMatchers.isNull(),
			eq(ExpenseContext.CASA),
			eq(10L),
			org.mockito.ArgumentMatchers.isNull(),
			eq(ExpenseStatus.ABERTA),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			org.mockito.ArgumentMatchers.isNull(),
			pageableCaptor.capture()
		);
		assertEquals(Sort.unsorted(), pageableCaptor.getValue().getSort());
	}

	@Test
	void deve_retornar_detalhe_com_pagamentos_e_saldo() {
		Expense expense = novaExpense(10L, "Mercado", "100.00", LocalDate.now().minusDays(1), ExpenseContext.CASA, 10L, 20L);
		Payment pagamento1 = novaPayment(1L, 10L, "40.00", PaymentMethod.PIX);
		Payment pagamento2 = novaPayment(2L, 10L, "30.00", PaymentMethod.DINHEIRO);

		when(expenseRepository.findByIdAndHouseholdId(10L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseId(10L)).thenReturn(List.of(pagamento1, pagamento2));

		ExpenseDetailResponse response = service.detalhar(10L);

		assertEquals(ExpenseStatus.PARCIALMENTE_PAGA, response.status());
		assertEquals(new BigDecimal("70.00"), response.paidAmount());
		assertEquals(new BigDecimal("30.00"), response.remainingAmount());
		assertEquals(2, response.paymentsCount());
		assertEquals(10L, response.category().id());
		assertEquals(20L, response.subcategory().id());
	}

	@Test
	void deve_atualizar_despesa_existente() {
		Expense expense = novaExpense(11L, "Mercado", "100.00", LocalDate.now().minusDays(1), ExpenseContext.CASA, 10L, 20L);
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(11L, 7L)).thenReturn(Optional.of(expense));
		when(categoryRepository.findById(7L, 10L)).thenReturn(Optional.of(new Category(10L, "Moradia", true)));
		when(subcategoryRepository.findById(7L, 20L)).thenReturn(Optional.of(new Subcategory(20L, 10L, "Internet", true)));
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(paymentRepository.findByExpenseId(11L)).thenReturn(List.of());

		ExpenseDetailResponse response = service.atualizar(
			11L,
			new UpdateExpenseRequest(
				"Mercado atualizado",
				new BigDecimal("150.00"),
				LocalDate.now(),
				LocalDate.now().plusDays(2),
				ExpenseContext.CASA,
				10L,
				20L,
				null,
				"Nova observacao"
			)
		);

		assertEquals("Mercado atualizado", response.description());
		assertEquals(new BigDecimal("150.00"), response.amount());
		assertEquals(ExpenseStatus.PREVISTA, response.status());
	}

	@Test
	void deve_rejeitar_atualizacao_quando_novo_valor_for_menor_que_total_pago() {
		Expense expense = novaExpense(12L, "Mercado", "100.00", LocalDate.now().minusDays(1), ExpenseContext.CASA, 10L, 20L);
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(12L, 7L)).thenReturn(Optional.of(expense));
		when(categoryRepository.findById(7L, 10L)).thenReturn(Optional.of(new Category(10L, "Moradia", true)));
		when(subcategoryRepository.findById(7L, 20L)).thenReturn(Optional.of(new Subcategory(20L, 10L, "Internet", true)));
		when(paymentRepository.findByExpenseId(12L)).thenReturn(List.of(
			novaPayment(1L, 12L, "40.00", PaymentMethod.PIX),
			novaPayment(2L, 12L, "30.00", PaymentMethod.DINHEIRO)
		));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.atualizar(
			12L,
			new UpdateExpenseRequest(
				"Mercado atualizado",
				new BigDecimal("60.00"),
				LocalDate.now(),
				LocalDate.now().plusDays(2),
				ExpenseContext.CASA,
				10L,
				20L,
				null,
				"Nova observacao"
			)
		));

		assertEquals("Expense amount cannot be lower than the paid amount", exception.getMessage());
	}

	@Test
	void deve_lancar_excecao_quando_despesa_nao_existir() {
		when(expenseRepository.findByIdAndHouseholdId(99L, 7L)).thenReturn(Optional.empty());

		assertThrows(ExpenseNotFoundException.class, () -> service.detalhar(99L));
	}

	@Test
	void deve_excluir_despesa_sem_pagamentos() {
		Expense expense = novaExpense(13L, "Mercado", "100.00", LocalDate.now(), ExpenseContext.CASA, 10L, 20L);
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(13L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseId(13L)).thenReturn(List.of());
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deletar(13L);

		assertNotNull(expense.getDeletedAt());
		verify(expenseRepository).save(expense);
	}

	@Test
	void deve_rejeitar_exclusao_quando_despesa_possuir_pagamentos() {
		Expense expense = novaExpense(14L, "Mercado", "100.00", LocalDate.now(), ExpenseContext.CASA, 10L, 20L);
		when(expenseRepository.findByIdAndHouseholdIdForUpdate(14L, 7L)).thenReturn(Optional.of(expense));
		when(paymentRepository.findByExpenseId(14L)).thenReturn(List.of(
			novaPayment(1L, 14L, "40.00", PaymentMethod.PIX)
		));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.deletar(14L));

		assertEquals("Expense with payments cannot be deleted", exception.getMessage());
	}

	private Expense novaExpense(Long id, String descricao, String valor, LocalDate vencimento, ExpenseContext context, Long categoryId, Long subcategoryId) {
		Expense expense = new Expense(
			descricao,
			new BigDecimal(valor),
			vencimento,
			context,
			categoryId,
			"Moradia",
			subcategoryId,
			"Internet",
			"Observacao"
		);
		expense.setId(id);
		return expense;
	}

	private Payment novaPayment(Long id, Long expenseId, String valor, PaymentMethod method) {
		Payment payment = new Payment(
			expenseId,
			new BigDecimal(valor),
			LocalDate.now(),
			method,
			"Pagamento"
		);
		payment.setId(id);
		return payment;
	}
}
