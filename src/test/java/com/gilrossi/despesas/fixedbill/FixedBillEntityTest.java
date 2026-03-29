package com.gilrossi.despesas.fixedbill;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.gilrossi.despesas.expense.ExpenseContext;

class FixedBillEntityTest {

	@Test
	void deve_manter_dados_e_timestamps() {
		FixedBill fixedBill = new FixedBill(
			7L,
			"Internet fibra",
			new BigDecimal("129.90"),
			LocalDate.of(2026, 4, 10),
			FixedBillFrequency.MONTHLY,
			ExpenseContext.CASA,
			10L,
			"Moradia",
			20L,
			"Internet",
			30L
		);
		fixedBill.setId(1L);
		fixedBill.prePersist();
		Instant createdAt = fixedBill.getCreatedAt();
		Instant updatedAt = fixedBill.getUpdatedAt();

		fixedBill.setActive(false);
		fixedBill.preUpdate();

		assertThat(fixedBill.getId()).isEqualTo(1L);
		assertThat(fixedBill.getHouseholdId()).isEqualTo(7L);
		assertThat(fixedBill.getDescription()).isEqualTo("Internet fibra");
		assertThat(fixedBill.getAmount()).isEqualByComparingTo("129.90");
		assertThat(fixedBill.getFirstDueDate()).isEqualTo(LocalDate.of(2026, 4, 10));
		assertThat(fixedBill.getFrequency()).isEqualTo(FixedBillFrequency.MONTHLY);
		assertThat(fixedBill.getContext()).isEqualTo(ExpenseContext.CASA);
		assertThat(fixedBill.getCategoryId()).isEqualTo(10L);
		assertThat(fixedBill.getCategoryNameSnapshot()).isEqualTo("Moradia");
		assertThat(fixedBill.getSubcategoryId()).isEqualTo(20L);
		assertThat(fixedBill.getSubcategoryNameSnapshot()).isEqualTo("Internet");
		assertThat(fixedBill.getSpaceReferenceId()).isEqualTo(30L);
		assertThat(fixedBill.isActive()).isFalse();
		assertThat(fixedBill.getCreatedAt()).isEqualTo(createdAt);
		assertThat(fixedBill.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}
}
