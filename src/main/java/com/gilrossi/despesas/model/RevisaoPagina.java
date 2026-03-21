package com.gilrossi.despesas.model;

import java.util.List;

public record RevisaoPagina(List<RevisaoPendencia> pendencias) {

	public int totalPendencias() {
		return pendencias == null ? 0 : pendencias.size();
	}

	public boolean hasPendencias() {
		return totalPendencias() > 0;
	}
}
