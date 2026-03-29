package com.gilrossi.despesas.fixedbill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.gilrossi.despesas.expense.ExpenseContext;

class FixedBillTest {

	@Test
	void deve_expor_estado_mutavel_e_ciclo_de_vida() {
		FixedBill fixedBill = new FixedBill(
			10L,
			"Internet",
			new BigDecimal("149.90"),
			LocalDate.of(2026, 3, 5),
			FixedBillFrequency.MONTHLY,
			ExpenseContext.CASA,
			1L,
			"Moradia",
			2L,
			"Internet",
			3L
		);

		assertTrue(fixedBill.isActive());
		assertNotNull(fixedBill.getCreatedAt());
		assertNotNull(fixedBill.getUpdatedAt());

		fixedBill.setId(99L);
		fixedBill.setHouseholdId(11L);
		fixedBill.setDescription("Internet fibra");
		fixedBill.setAmount(new BigDecimal("159.90"));
		fixedBill.setFirstDueDate(LocalDate.of(2026, 3, 8));
		fixedBill.setFrequency(FixedBillFrequency.MONTHLY);
		fixedBill.setContext(ExpenseContext.VEICULO);
		fixedBill.setCategoryId(4L);
		fixedBill.setCategoryNameSnapshot("Serviços");
		fixedBill.setSubcategoryId(5L);
		fixedBill.setSubcategoryNameSnapshot("Conectividade");
		fixedBill.setSpaceReferenceId(6L);
		fixedBill.setActive(false);
		fixedBill.setCreatedAt(Instant.parse("2026-03-01T10:00:00Z"));
		fixedBill.setUpdatedAt(Instant.parse("2026-03-01T11:00:00Z"));

		assertEquals(99L, fixedBill.getId());
		assertEquals(11L, fixedBill.getHouseholdId());
		assertEquals("Internet fibra", fixedBill.getDescription());
		assertEquals(new BigDecimal("159.90"), fixedBill.getAmount());
		assertEquals(LocalDate.of(2026, 3, 8), fixedBill.getFirstDueDate());
		assertEquals(FixedBillFrequency.MONTHLY, fixedBill.getFrequency());
		assertEquals(ExpenseContext.VEICULO, fixedBill.getContext());
		assertEquals(4L, fixedBill.getCategoryId());
		assertEquals("Serviços", fixedBill.getCategoryNameSnapshot());
		assertEquals(5L, fixedBill.getSubcategoryId());
		assertEquals("Conectividade", fixedBill.getSubcategoryNameSnapshot());
		assertEquals(6L, fixedBill.getSpaceReferenceId());
		assertFalse(fixedBill.isActive());
		assertEquals(Instant.parse("2026-03-01T10:00:00Z"), fixedBill.getCreatedAt());
		assertEquals(Instant.parse("2026-03-01T11:00:00Z"), fixedBill.getUpdatedAt());

		fixedBill.prePersist();
		Instant persistedUpdatedAt = fixedBill.getUpdatedAt();
		assertFalse(persistedUpdatedAt.isBefore(fixedBill.getCreatedAt()));

		fixedBill.preUpdate();
		assertFalse(fixedBill.getUpdatedAt().isBefore(persistedUpdatedAt));
	}
}
