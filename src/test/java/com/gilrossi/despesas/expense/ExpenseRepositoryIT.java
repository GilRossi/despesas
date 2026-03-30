package com.gilrossi.despesas.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;

@SpringBootTest
@ActiveProfiles("postgres-it")
public class ExpenseRepositoryIT {

	private static final Long HOUSEHOLD_ID = 1L;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void deve_persistir_e_recuperar_despesa_no_postgresql() {
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category category = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Moradia", true));
		Subcategory subcategory = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Internet", true));

		Expense expense = new Expense(
			HOUSEHOLD_ID,
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.now().plusDays(10),
			ExpenseContext.CASA,
			category.getId(),
			category.getName(),
			subcategory.getId(),
			subcategory.getName(),
			"Conta mensal"
		);

		Expense saved = expenseRepository.save(expense);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();

		Expense persisted = expenseRepository.findById(saved.getId()).orElseThrow();

		assertThat(persisted.getDescription()).isEqualTo("Internet da casa");
		assertThat(persisted.getAmount()).isEqualByComparingTo("120.00");
		assertThat(persisted.getDueDate()).isEqualTo(LocalDate.now().plusDays(10));
		assertThat(persisted.getContext()).isEqualTo(ExpenseContext.CASA);
		assertThat(persisted.getCategoryId()).isEqualTo(category.getId());
		assertThat(persisted.getCategoryNameSnapshot()).isEqualTo("Moradia");
		assertThat(persisted.getSubcategoryId()).isEqualTo(subcategory.getId());
		assertThat(persisted.getSubcategoryNameSnapshot()).isEqualTo("Internet");
	}

	@Test
	void deve_filtrar_e_paginar_despesas_no_postgresql() {
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category category = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));
		Subcategory mercado = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Mercado", true));
		Subcategory internet = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Internet", true));

		Expense overdueNewest = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Mercado do mês",
			new BigDecimal("180.00"),
			LocalDate.now().minusDays(1),
			ExpenseContext.CASA,
			category.getId(),
			category.getName(),
			mercado.getId(),
			mercado.getName(),
			"Compra da semana"
		));
		Expense overdueOldest = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Mercado anterior",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(4),
			ExpenseContext.CASA,
			category.getId(),
			category.getName(),
			mercado.getId(),
			mercado.getName(),
			"Compra anterior"
		));
		expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Internet",
			new BigDecimal("90.00"),
			LocalDate.now().plusDays(3),
			ExpenseContext.CASA,
			category.getId(),
			category.getName(),
			internet.getId(),
			internet.getName(),
			"Conta futura"
		));

		Page<Expense> page = expenseRepository.findAllByFilters(
			HOUSEHOLD_ID,
			"mercado",
			ExpenseContext.CASA,
			category.getId(),
			mercado.getId(),
			ExpenseStatus.VENCIDA,
			true,
			null,
			null,
			false,
			PageRequest.of(0, 1)
		);

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().getFirst().getId()).isEqualTo(overdueOldest.getId());
		assertThat(page.getContent().getFirst().getDescription()).isEqualTo("Mercado anterior");
	}

	@Test
	void deve_listar_mais_recente_primeiro_pelo_momento_do_lancamento() {
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
		Category category = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Moradia", true));
		Subcategory subcategory = subcategoryRepository.save(HOUSEHOLD_ID, new Subcategory(null, category.getId(), "Aluguel", true));

		Expense olderOccurrenceCreatedFirst = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Despesa antiga criada antes",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(1),
			null,
			ExpenseContext.GERAL,
			category.getId(),
			category.getName(),
			subcategory.getId(),
			subcategory.getName(),
			"",
			null
		));
		Expense newerCreatedLast = expenseRepository.save(new Expense(
			HOUSEHOLD_ID,
			"Despesa retroativa criada por ultimo",
			new BigDecimal("89.90"),
			LocalDate.now().minusDays(30),
			null,
			ExpenseContext.GERAL,
			category.getId(),
			category.getName(),
			subcategory.getId(),
			subcategory.getName(),
			"",
			null
		));

		Page<Expense> page = expenseRepository.findAllByFilters(
			HOUSEHOLD_ID,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			PageRequest.of(0, 20)
		);

		assertThat(page.getContent()).hasSize(2);
		assertThat(page.getContent().get(0).getId()).isEqualTo(newerCreatedLast.getId());
		assertThat(page.getContent().get(0).getDescription()).isEqualTo("Despesa retroativa criada por ultimo");
		assertThat(page.getContent().get(1).getId()).isEqualTo(olderOccurrenceCreatedFirst.getId());
	}
}
