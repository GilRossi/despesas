package com.gilrossi.despesas.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ExpenseEntityTest {

	@Test
	void deve_preservar_estado_e_timestamps() {
		Expense expense = new Expense(
			7L,
			"Internet",
			new BigDecimal("120.00"),
			LocalDate.of(2026, 3, 31),
			ExpenseContext.CASA,
			10L,
			"Moradia",
			20L,
			"Internet",
			"conta"
		);
		expense.setId(1L);
		expense.prePersist();
		Instant createdAt = expense.getCreatedAt();
		Instant updatedAt = expense.getUpdatedAt();

		expense.touch();
		expense.markDeleted();

		assertThat(expense.getId()).isEqualTo(1L);
		assertThat(expense.getHouseholdId()).isEqualTo(7L);
		assertThat(expense.getDescription()).isEqualTo("Internet");
		assertThat(expense.getAmount()).isEqualByComparingTo("120.00");
		assertThat(expense.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 31));
		assertThat(expense.getContext()).isEqualTo(ExpenseContext.CASA);
		assertThat(expense.getCategoryId()).isEqualTo(10L);
		assertThat(expense.getCategoryNameSnapshot()).isEqualTo("Moradia");
		assertThat(expense.getSubcategoryId()).isEqualTo(20L);
		assertThat(expense.getSubcategoryNameSnapshot()).isEqualTo("Internet");
		assertThat(expense.getNotes()).isEqualTo("conta");
		assertThat(expense.getCreatedAt()).isEqualTo(createdAt);
		assertThat(expense.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
		assertThat(expense.getDeletedAt()).isNotNull();
		expense.setStatus(ExpenseStatus.PREVISTA);
		assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.PREVISTA);
		expense.setDeletedAt(Instant.parse("2026-03-29T14:00:00Z"));
		assertThat(expense.getDeletedAt()).isEqualTo(Instant.parse("2026-03-29T14:00:00Z"));
	}
}
