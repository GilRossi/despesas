package com.gilrossi.despesas.income;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class IncomeEntityTest {

	@Test
	void deve_manter_dados_e_timestamps() {
		Income income = new Income(7L, "Salario", new BigDecimal("3500.00"), LocalDate.of(2026, 3, 31), 30L);
		income.setId(1L);
		income.prePersist();
		Instant createdAt = income.getCreatedAt();
		Instant updatedAt = income.getUpdatedAt();

		income.setDescription("Salario atualizado");
		income.preUpdate();

		assertThat(income.getId()).isEqualTo(1L);
		assertThat(income.getHouseholdId()).isEqualTo(7L);
		assertThat(income.getDescription()).isEqualTo("Salario atualizado");
		assertThat(income.getAmount()).isEqualByComparingTo("3500.00");
		assertThat(income.getReceivedOn()).isEqualTo(LocalDate.of(2026, 3, 31));
		assertThat(income.getSpaceReferenceId()).isEqualTo(30L);
		assertThat(income.getCreatedAt()).isEqualTo(createdAt);
		assertThat(income.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}
}
