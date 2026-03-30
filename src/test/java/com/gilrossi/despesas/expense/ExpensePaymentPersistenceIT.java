package com.gilrossi.despesas.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.payment.CreatePaymentRequest;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentResponse;
import com.gilrossi.despesas.payment.PaymentService;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("postgres-it")
public class ExpensePaymentPersistenceIT {

	private static final Long HOUSEHOLD_ID = 1L;

	@Autowired
	private ExpenseService expenseService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private CurrentHouseholdProvider currentHouseholdProvider;

	@Test
	void deve_criar_despesa_ja_paga_no_mesmo_lancamento() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(HOUSEHOLD_ID);
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category category = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Moradia", true));
		Subcategory subcategory = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Internet", true));

		ExpenseResponse expense = expenseService.criar(new CreateExpenseRequest(
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(1),
			LocalDate.now().plusDays(2),
			category.getId(),
			subcategory.getId(),
			null,
			"Conta mensal",
			new CreateExpenseInitialPaymentRequest(LocalDate.now(), PaymentMethod.PIX)
		));

		assertThat(expense.id()).isNotNull();
		assertThat(expense.status()).isEqualTo(ExpenseStatus.PAGA);
		assertThat(expense.remainingAmount()).isEqualByComparingTo("0.00");
		assertThat(expense.paidAmount()).isEqualByComparingTo("120.00");

		ExpenseDetailResponse detalhada = expenseService.detalhar(expense.id());

		assertThat(detalhada.status()).isEqualTo(ExpenseStatus.PAGA);
		assertThat(detalhada.paymentsCount()).isEqualTo(1);
		assertThat(detalhada.remainingAmount()).isEqualByComparingTo("0.00");
		assertThat(detalhada.paidAmount()).isEqualByComparingTo("120.00");
	}

	@Test
	void deve_registrar_pagamento_no_banco_e_recalcular_saldo_da_despesa() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(HOUSEHOLD_ID);
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category category = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Moradia", true));
		Subcategory subcategory = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Internet", true));

		ExpenseResponse expense = expenseService.criar(new CreateExpenseRequest(
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(1),
			LocalDate.now().minusDays(1),
			category.getId(),
			subcategory.getId(),
			null,
			"Conta mensal"
		));

		assertThat(expense.id()).isNotNull();

		PaymentResponse payment = paymentService.registrar(new CreatePaymentRequest(
			expense.id(),
			new BigDecimal("40.00"),
			LocalDate.now(),
			PaymentMethod.PIX,
			"Primeira parcela"
		));

		assertThat(payment.id()).isNotNull();
		assertThat(payment.expenseStatus()).isEqualTo(ExpenseStatus.PARCIALMENTE_PAGA);
		assertThat(payment.expenseRemainingAmount()).isEqualByComparingTo("80.00");

		ExpenseDetailResponse detalhada = expenseService.detalhar(expense.id());

		assertThat(detalhada.paidAmount()).isEqualByComparingTo("40.00");
		assertThat(detalhada.remainingAmount()).isEqualByComparingTo("80.00");
		assertThat(detalhada.paymentsCount()).isEqualTo(1);
		assertThat(detalhada.status()).isEqualTo(ExpenseStatus.PARCIALMENTE_PAGA);
	}
}
