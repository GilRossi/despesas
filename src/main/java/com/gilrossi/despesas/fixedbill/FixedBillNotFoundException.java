package com.gilrossi.despesas.fixedbill;

public class FixedBillNotFoundException extends RuntimeException {

	public FixedBillNotFoundException(Long id) {
		super("Fixed bill with id " + id + " was not found");
	}
}
