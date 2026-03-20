package com.gilrossi.despesas.model;

public enum RelatorioAssistenteAcao {

	EXPLICAR_MES("EXPLICAR_MES", "Explicar este mês", "Como estou este mês?"),
	O_QUE_MUDOU("O_QUE_MUDOU", "O que mudou?", "O que aumentou em relação ao mês passado?"),
	COMO_ECONOMIZAR("COMO_ECONOMIZAR", "Como economizar?", "Como posso economizar este mês?");

	private final String code;
	private final String label;
	private final String question;

	RelatorioAssistenteAcao(String code, String label, String question) {
		this.code = code;
		this.label = label;
		this.question = question;
	}

	public String code() {
		return code;
	}

	public String label() {
		return label;
	}

	public String question() {
		return question;
	}

	public static RelatorioAssistenteAcao fromCode(String code) {
		for (RelatorioAssistenteAcao action : values()) {
			if (action.code.equalsIgnoreCase(code)) {
				return action;
			}
		}
		throw new IllegalArgumentException("Atalho do assistente inválido.");
	}
}
