package com.gilrossi.despesas.api.v1.space;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.spacereference.SpaceReference;
import com.gilrossi.despesas.spacereference.SpaceReferenceCreateResult;
import com.gilrossi.despesas.spacereference.SpaceReferenceService;
import com.gilrossi.despesas.spacereference.SpaceReferenceType;
import com.gilrossi.despesas.spacereference.SpaceReferenceTypeGroup;

@WebMvcTest(SpaceReferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class SpaceReferenceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SpaceReferenceService service;

	@Test
	void deve_listar_referencias_em_json() throws Exception {
		when(service.list(SpaceReferenceTypeGroup.VEICULOS, null, "carro"))
			.thenReturn(List.of(new SpaceReference(
				1L,
				10L,
				SpaceReferenceType.CARRO,
				"Carro de apoio",
				"carro de apoio",
				OffsetDateTime.parse("2026-03-28T12:00:00Z"),
				OffsetDateTime.parse("2026-03-28T12:00:00Z"),
				null
			)));

		mockMvc.perform(get("/api/v1/space/references")
				.param("typeGroup", "VEICULOS")
				.param("q", "carro"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].type").value("CARRO"))
			.andExpect(jsonPath("$.data[0].typeGroup").value("VEICULOS"))
			.andExpect(jsonPath("$.data[0].name").value("Carro de apoio"));
	}

	@Test
	void deve_criar_referencia_e_retornar_201() throws Exception {
		when(service.create(any())).thenReturn(SpaceReferenceCreateResult.created(new SpaceReference(
			1L,
			10L,
			SpaceReferenceType.ESCRITORIO,
			"Escritorio Central",
			"escritorio central",
			OffsetDateTime.parse("2026-03-28T12:00:00Z"),
			OffsetDateTime.parse("2026-03-28T12:00:00Z"),
			null
		)));

		mockMvc.perform(post("/api/v1/space/references")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type":"ESCRITORIO",
					  "name":"Escritorio Central"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.result").value("CREATED"))
			.andExpect(jsonPath("$.data.reference.type").value("ESCRITORIO"))
			.andExpect(jsonPath("$.data.reference.typeGroup").value("COMERCIAL_TRABALHO"))
			.andExpect(jsonPath("$.data.reference.name").value("Escritorio Central"));
	}

	@Test
	void deve_retornar_sugestao_de_duplicidade_sem_409() throws Exception {
		when(service.create(any())).thenReturn(SpaceReferenceCreateResult.duplicateSuggestion(
			new SpaceReference(
				8L,
				10L,
				SpaceReferenceType.CLIENTE,
				"Cliente Acme",
				"cliente acme",
				OffsetDateTime.parse("2026-03-28T12:00:00Z"),
				OffsetDateTime.parse("2026-03-28T12:00:00Z"),
				null
			),
			"Encontrei uma referência parecida no seu Espaço. Quer usar essa para evitar duplicidade?"
		));

		mockMvc.perform(post("/api/v1/space/references")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type":"CLIENTE",
					  "name":"Cliente Acme"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.result").value("DUPLICATE_SUGGESTION"))
			.andExpect(jsonPath("$.data.reference").doesNotExist())
			.andExpect(jsonPath("$.data.suggestedReference.id").value(8))
			.andExpect(jsonPath("$.data.message").value("Encontrei uma referência parecida no seu Espaço. Quer usar essa para evitar duplicidade?"));
	}

	@Test
	void deve_validar_payload_invalido() throws Exception {
		mockMvc.perform(post("/api/v1/space/references")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name":" "
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").exists());
	}
}
