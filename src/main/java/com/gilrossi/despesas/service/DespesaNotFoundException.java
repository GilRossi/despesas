package com.gilrossi.despesas.service;

public class DespesaNotFoundException extends RuntimeException {

	public DespesaNotFoundException(Long id) {
		super("Despesa com ID " + id + " não foi encontrada.");
	}
}
