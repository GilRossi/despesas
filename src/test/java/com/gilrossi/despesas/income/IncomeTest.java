package com.gilrossi.despesas.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class IncomeTest {

	@Test
	void deve_expor_estado_mutavel_e_ciclo_de_vida() {
		Income income = new Income(
			10L,
			"Salário",
			new BigDecimal("5000.00"),
			LocalDate.of(2026, 3, 20),
			2L
		);

		assertNotNull(income.getCreatedAt());
		assertNotNull(income.getUpdatedAt());

		income.setId(91L);
		income.setHouseholdId(11L);
		income.setDescription("Salário reajustado");
		income.setAmount(new BigDecimal("5500.00"));
		income.setReceivedOn(LocalDate.of(2026, 3, 25));
		income.setSpaceReferenceId(6L);
		income.setCreatedAt(Instant.parse("2026-03-01T10:00:00Z"));
		income.setUpdatedAt(Instant.parse("2026-03-01T11:00:00Z"));

		assertEquals(91L, income.getId());
		assertEquals(11L, income.getHouseholdId());
		assertEquals("Salário reajustado", income.getDescription());
		assertEquals(new BigDecimal("5500.00"), income.getAmount());
		assertEquals(LocalDate.of(2026, 3, 25), income.getReceivedOn());
		assertEquals(6L, income.getSpaceReferenceId());

		income.prePersist();
		Instant persistedUpdatedAt = income.getUpdatedAt();
		assertEquals(Instant.parse("2026-03-01T10:00:00Z"), income.getCreatedAt());
		assertFalse(persistedUpdatedAt.isBefore(income.getCreatedAt()));

		income.preUpdate();
		assertFalse(income.getUpdatedAt().isBefore(persistedUpdatedAt));
	}
}
