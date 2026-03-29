package com.gilrossi.despesas.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PaymentTest {

	@Test
	void deve_expor_estado_mutavel_e_ciclo_de_vida() {
		Payment payment = new Payment(
			10L,
			new BigDecimal("49.90"),
			LocalDate.of(2026, 3, 29),
			PaymentMethod.PIX,
			"Pagamento parcial"
		);

		assertNotNull(payment.getCreatedAt());
		assertNotNull(payment.getUpdatedAt());

		payment.setId(91L);
		payment.setCreatedAt(Instant.parse("2026-03-01T10:00:00Z"));
		payment.setUpdatedAt(Instant.parse("2026-03-01T11:00:00Z"));
		payment.setDeletedAt(Instant.parse("2026-03-02T12:00:00Z"));

		assertEquals(91L, payment.getId());
		assertEquals(10L, payment.getExpenseId());
		assertEquals(new BigDecimal("49.90"), payment.getAmount());
		assertEquals(LocalDate.of(2026, 3, 29), payment.getPaidAt());
		assertEquals(PaymentMethod.PIX, payment.getMethod());
		assertEquals("Pagamento parcial", payment.getNotes());
		assertEquals(Instant.parse("2026-03-01T10:00:00Z"), payment.getCreatedAt());
		assertEquals(Instant.parse("2026-03-01T11:00:00Z"), payment.getUpdatedAt());
		assertEquals(Instant.parse("2026-03-02T12:00:00Z"), payment.getDeletedAt());

		Instant touchedAt = payment.getUpdatedAt();
		payment.touch();
		assertFalse(payment.getUpdatedAt().isBefore(touchedAt));

		payment.prePersist();
		Instant persistedUpdatedAt = payment.getUpdatedAt();
		assertFalse(persistedUpdatedAt.isBefore(payment.getCreatedAt()));

		payment.preUpdate();
		assertFalse(payment.getUpdatedAt().isBefore(persistedUpdatedAt));
	}
}
