package com.gilrossi.despesas.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;

@SpringBootTest
@ActiveProfiles("postgres-it")
public class PaymentRepositoryIT {

	private static final Long HOUSEHOLD_ID = 1L;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void deve_persistir_pagamentos_e_recuperar_por_despesa_em_ordem_descendente() {
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category category = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Carro", true));
		Subcategory combustivel = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Combustivel", true));
		Expense expense = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Combustivel",
			new BigDecimal("300.00"),
			LocalDate.now().minusDays(1),
			ExpenseContext.VEICULO,
			category.getId(),
			category.getName(),
			combustivel.getId(),
			combustivel.getName(),
			"Abastecimento semanal"
		));

		Payment payment1 = paymentRepository.save(new Payment(
			expense.getId(),
			new BigDecimal("100.00"),
			LocalDate.now().minusDays(2),
			PaymentMethod.PIX,
			"Primeiro pagamento"
		));
		Payment payment2 = paymentRepository.save(new Payment(
			expense.getId(),
			new BigDecimal("50.00"),
			LocalDate.now(),
			PaymentMethod.DINHEIRO,
			"Segundo pagamento"
		));

		List<Payment> payments = paymentRepository.findByExpenseId(expense.getId());

		assertThat(payments).extracting(Payment::getId).containsExactly(payment2.getId(), payment1.getId());
		assertThat(payments).extracting(Payment::getAmount).containsExactly(new BigDecimal("50.00"), new BigDecimal("100.00"));
		assertThat(payments).allMatch(payment -> payment.getCreatedAt() != null && payment.getUpdatedAt() != null);
	}

	@Test
	void deve_recuperar_pagamentos_de_varias_despesas_em_ordem_deterministica() {
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category casa = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));
		Subcategory internet = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, casa.getId(), "Internet", true));
		Category carro = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Carro", true));
		Subcategory combustivel = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, carro.getId(), "Combustivel", true));
		Expense expense1 = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Internet",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(1),
			ExpenseContext.CASA,
			casa.getId(),
			casa.getName(),
			internet.getId(),
			internet.getName(),
			"Conta mensal"
		));
		Expense expense2 = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Combustivel",
			new BigDecimal("300.00"),
			LocalDate.now().minusDays(2),
			ExpenseContext.VEICULO,
			carro.getId(),
			carro.getName(),
			combustivel.getId(),
			combustivel.getName(),
			"Abastecimento"
		));

		Payment payment11 = paymentRepository.save(new Payment(
			expense1.getId(),
			new BigDecimal("40.00"),
			LocalDate.now().minusDays(2),
			PaymentMethod.PIX,
			"Parcela 1"
		));
		Payment payment12 = paymentRepository.save(new Payment(
			expense1.getId(),
			new BigDecimal("30.00"),
			LocalDate.now(),
			PaymentMethod.DINHEIRO,
			"Parcela 2"
		));
		Payment payment21 = paymentRepository.save(new Payment(
			expense2.getId(),
			new BigDecimal("100.00"),
			LocalDate.now().minusDays(1),
			PaymentMethod.PIX,
			"Parcela 1"
		));

		List<Payment> payments = paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(List.of(expense2.getId(), expense1.getId()));

		assertThat(payments).extracting(Payment::getId).containsExactly(
			payment12.getId(),
			payment11.getId(),
			payment21.getId()
		);
		assertThat(payments).extracting(Payment::getExpenseId).containsExactly(
			expense1.getId(),
			expense1.getId(),
			expense2.getId()
		);
	}
}
