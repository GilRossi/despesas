package com.gilrossi.despesas.api.v1.emailingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.gilrossi.despesas.emailingestion.EmailIngestionSource;
import com.gilrossi.despesas.emailingestion.EmailIngestionSourceService;

@WebMvcTest(EmailIngestionSourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class EmailIngestionSourceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmailIngestionSourceService service;

	@Test
	void deve_listar_sources_do_household() throws Exception {
		when(service.list()).thenReturn(List.of(new EmailIngestionSource(
			10L,
			20L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"Gmail pessoal",
			true,
			new BigDecimal("0.90"),
			new BigDecimal("0.65"),
			OffsetDateTime.parse("2026-03-20T10:15:30Z"),
			OffsetDateTime.parse("2026-03-20T10:15:30Z")
		)));

		mockMvc.perform(get("/api/v1/email-ingestion/sources"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].sourceAccount").value("financeiro@gmail.com"))
			.andExpect(jsonPath("$.data[0].autoImportMinConfidence").value(0.90));
	}

	@Test
	void deve_criar_source_e_retornar_201() throws Exception {
		when(service.register(any())).thenReturn(new EmailIngestionSource(
			10L,
			20L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"Gmail pessoal",
			true,
			new BigDecimal("0.90"),
			new BigDecimal("0.65"),
			OffsetDateTime.parse("2026-03-20T10:15:30Z"),
			OffsetDateTime.parse("2026-03-20T10:15:30Z")
		));

		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro@gmail.com",
					  "label":"Gmail pessoal"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.sourceAccount").value("financeiro@gmail.com"));
	}

	@Test
	void deve_validar_source_account_obrigatorio() throws Exception {
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":" ",
					  "label":"Gmail pessoal"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("sourceAccount"));
	}
}
