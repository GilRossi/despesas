package com.gilrossi.despesas.payment;

public class PaymentNotFoundException extends RuntimeException {

	public PaymentNotFoundException(Long id) {
		super("Payment with id " + id + " was not found");
	}
}
