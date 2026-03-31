package com.gilrossi.despesas.api.v1.fixedbill;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.api.v1.shared.FieldBusinessRuleException;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.fixedbill.CreateFixedBillRequest;
import com.gilrossi.despesas.fixedbill.FixedBillFrequency;
import com.gilrossi.despesas.fixedbill.FixedBillResponse;
import com.gilrossi.despesas.fixedbill.FixedBillService;

@WebMvcTest(FixedBillApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class FixedBillApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FixedBillService fixedBillService;

	@Test
	void deve_criar_conta_fixa_e_retornar_resposta_minima() throws Exception {
		when(fixedBillService.create(any(CreateFixedBillRequest.class))).thenReturn(new FixedBillResponse(
			15L,
			"Internet fibra",
			new BigDecimal("129.90"),
			LocalDate.of(2026, 4, 10),
			FixedBillFrequency.MONTHLY,
			new ReferenceResponse(10L, "Casa"),
			new ReferenceResponse(20L, "Internet"),
			new ReferenceResponse(30L, "Apartamento Centro"),
			true,
			Instant.parse("2026-03-28T12:00:00Z")
		));

		mockMvc.perform(post("/api/v1/fixed-bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"Internet fibra",
					  "amount":129.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"MONTHLY",
					  "categoryId":10,
					  "subcategoryId":20,
					  "spaceReferenceId":30
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(15))
			.andExpect(jsonPath("$.data.description").value("Internet fibra"))
			.andExpect(jsonPath("$.data.frequency").value("MONTHLY"))
			.andExpect(jsonPath("$.data.category.id").value(10))
			.andExpect(jsonPath("$.data.subcategory.id").value(20))
			.andExpect(jsonPath("$.data.spaceReference.id").value(30))
			.andExpect(jsonPath("$.data.active").value(true))
			.andExpect(jsonPath("$.data.createdAt").value("2026-03-28T12:00:00Z"));
	}

	@Test
	void deve_rejeitar_payload_invalido_quando_descricao_estiver_vazia() throws Exception {
		mockMvc.perform(post("/api/v1/fixed-bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"",
					  "amount":129.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"MONTHLY",
					  "categoryId":10,
					  "subcategoryId":20
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
	}

	@Test
	void deve_retornar_field_error_quando_subcategoria_for_incompativel() throws Exception {
		when(fixedBillService.create(any(CreateFixedBillRequest.class))).thenThrow(
			new FieldBusinessRuleException(
				"subcategoryId",
				"subcategoryId must belong to the informed category"
			)
		);

		mockMvc.perform(post("/api/v1/fixed-bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"Internet fibra",
					  "amount":129.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"MONTHLY",
					  "categoryId":10,
					  "subcategoryId":99
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("subcategoryId"));
	}

	@Test
	void deve_aceitar_payload_semanal_no_contrato() throws Exception {
		when(fixedBillService.create(any(CreateFixedBillRequest.class))).thenReturn(new FixedBillResponse(
			18L,
			"Faxina",
			new BigDecimal("90.00"),
			LocalDate.of(2026, 4, 3),
			FixedBillFrequency.WEEKLY,
			new ReferenceResponse(10L, "Moradia"),
			new ReferenceResponse(21L, "Condomínio"),
			null,
			true,
			Instant.parse("2026-03-30T12:00:00Z")
		));

		mockMvc.perform(post("/api/v1/fixed-bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"Faxina",
					  "amount":90.00,
					  "firstDueDate":"2026-04-03",
					  "frequency":"WEEKLY",
					  "categoryId":10,
					  "subcategoryId":21
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(18))
			.andExpect(jsonPath("$.data.frequency").value("WEEKLY"));
	}
}
