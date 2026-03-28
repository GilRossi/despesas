package com.gilrossi.despesas.spacereference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class SpaceReferenceServiceTest {

	@Mock
	private SpaceReferenceRepository repository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private SpaceReferenceService service;

	@BeforeEach
	void setUp() {
		service = new SpaceReferenceService(repository, currentHouseholdProvider);
	}

	@Test
	void deve_criar_referencia_normalizando_nome() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
		when(repository.findByTypeAndNormalizedName(7L, SpaceReferenceType.ESCRITORIO, "escritorio central"))
			.thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(invocation -> {
			SpaceReference reference = invocation.getArgument(0);
			reference.setId(11L);
			reference.setCreatedAt(OffsetDateTime.parse("2026-03-28T12:00:00Z"));
			reference.setUpdatedAt(OffsetDateTime.parse("2026-03-28T12:00:00Z"));
			return reference;
		});

		SpaceReferenceCreateResult result = service.create(
			new CreateSpaceReferenceCommand(SpaceReferenceType.ESCRITORIO, "  Escritório   Central  ")
		);

		assertEquals(SpaceReferenceCreateResultType.CREATED, result.result());
		assertEquals("Escritório Central", result.reference().getName());
		assertEquals("escritorio central", result.reference().getNormalizedName());
		assertEquals(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO, result.reference().getTypeGroup());
		assertNull(result.message());
	}

	@Test
	void deve_retornar_sugestao_quando_duplicidade_existir() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
		SpaceReference existing = new SpaceReference(
			5L,
			7L,
			SpaceReferenceType.CLIENTE,
			"Cliente Acme",
			"cliente acme",
			OffsetDateTime.parse("2026-03-28T12:00:00Z"),
			OffsetDateTime.parse("2026-03-28T12:00:00Z"),
			null
		);
		when(repository.findByTypeAndNormalizedName(7L, SpaceReferenceType.CLIENTE, "cliente acme"))
			.thenReturn(Optional.of(existing));

		SpaceReferenceCreateResult result = service.create(
			new CreateSpaceReferenceCommand(SpaceReferenceType.CLIENTE, "  cliente   ácme ")
		);

		assertEquals(SpaceReferenceCreateResultType.DUPLICATE_SUGGESTION, result.result());
		assertSame(existing, result.suggestedReference());
		assertEquals("Encontrei uma referência parecida no seu Espaço. Quer usar essa para evitar duplicidade?", result.message());
	}

	@Test
	void deve_tratar_corrida_de_unicidade_com_sugestao_estruturada() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
		SpaceReference existing = new SpaceReference(
			8L,
			7L,
			SpaceReferenceType.CASA,
			"Casa da Praia",
			"casa da praia",
			OffsetDateTime.parse("2026-03-28T12:00:00Z"),
			OffsetDateTime.parse("2026-03-28T12:00:00Z"),
			null
		);
		when(repository.findByTypeAndNormalizedName(7L, SpaceReferenceType.CASA, "casa da praia"))
			.thenReturn(Optional.empty(), Optional.of(existing));
		when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

		SpaceReferenceCreateResult result = service.create(
			new CreateSpaceReferenceCommand(SpaceReferenceType.CASA, "Casa da Praia")
		);

		assertEquals(SpaceReferenceCreateResultType.DUPLICATE_SUGGESTION, result.result());
		assertSame(existing, result.suggestedReference());
	}

	@Test
	void deve_listar_referencias_filtradas() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(7L);
		SpaceReference reference = new SpaceReference(
			3L,
			7L,
			SpaceReferenceType.CARRO,
			"Carro do trabalho",
			"carro do trabalho",
			null,
			null,
			null
		);
		when(repository.findAll(7L, SpaceReferenceTypeGroup.VEICULOS, null, "carro"))
			.thenReturn(List.of(reference));

		List<SpaceReference> result = service.list(SpaceReferenceTypeGroup.VEICULOS, null, "carro");

		assertEquals(1, result.size());
		assertSame(reference, result.getFirst());
	}

	@Test
	void deve_retornar_lista_vazia_quando_tipo_nao_pertencer_ao_grupo() {
		List<SpaceReference> result = service.list(
			SpaceReferenceTypeGroup.RESIDENCIAL,
			SpaceReferenceType.CARRO,
			null
		);

		assertTrue(result.isEmpty());
		verify(repository, org.mockito.Mockito.never()).findAll(any(), any(), any(), any());
	}
}
