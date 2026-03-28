package com.gilrossi.despesas.spacereference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SpaceReferenceNormalizerTest {

	@Test
	void deve_normalizar_nome_com_trim_acentos_caixa_e_espacos() {
		assertEquals(
			"escritorio central",
			SpaceReferenceNormalizer.normalizeName("  Escritório   Central  ")
		);
	}

	@Test
	void deve_sanitizar_nome_para_exibicao_sem_duplicar_espacos() {
		assertEquals(
			"Escritório Central",
			SpaceReferenceNormalizer.sanitizeDisplayName("  Escritório   Central  ")
		);
	}

	@Test
	void deve_retornar_nulo_quando_nome_for_vazio() {
		assertNull(SpaceReferenceNormalizer.normalizeName("   "));
		assertNull(SpaceReferenceNormalizer.sanitizeDisplayName("   "));
	}
}
