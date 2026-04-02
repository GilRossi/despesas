package com.gilrossi.despesas.api.v1.fixedbill;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.gilrossi.despesas.api.v1.shared.FieldBusinessRuleException;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.fixedbill.CreateFixedBillRequest;
import com.gilrossi.despesas.fixedbill.FixedBillFrequency;
import com.gilrossi.despesas.fixedbill.FixedBillGeneratedExpenseResponse;
import com.gilrossi.despesas.fixedbill.FixedBillOperationalStatus;
import com.gilrossi.despesas.fixedbill.FixedBillResponse;
import com.gilrossi.despesas.fixedbill.FixedBillService;
import com.gilrossi.despesas.fixedbill.UpdateFixedBillRequest;

@WebMvcTest(FixedBillApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class FixedBillApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FixedBillService fixedBillService;

	@Test
	void deve_criar_conta_fixa_e_retornar_resposta_operacional() throws Exception {
		when(fixedBillService.create(any(CreateFixedBillRequest.class))).thenReturn(fixedBillResponse());

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
			.andExpect(jsonPath("$.data.operationalStatus").value("UPCOMING"))
			.andExpect(jsonPath("$.data.nextDueDate").value("2026-04-10"));
	}

	@Test
	void deve_listar_contas_fixas_ativas() throws Exception {
		when(fixedBillService.listActive()).thenReturn(List.of(fixedBillResponse()));

		mockMvc.perform(get("/api/v1/fixed-bills"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").value(15))
			.andExpect(jsonPath("$.data[0].description").value("Internet fibra"))
			.andExpect(jsonPath("$.data[0].spaceReference.name").value("Apartamento Centro"))
			.andExpect(jsonPath("$.data[0].lastGeneratedExpense.expenseId").value(90));
	}

	@Test
	void deve_detalhar_conta_fixa_operacional() throws Exception {
		when(fixedBillService.detail(15L)).thenReturn(fixedBillResponse());

		mockMvc.perform(get("/api/v1/fixed-bills/15"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(15))
			.andExpect(jsonPath("$.data.nextDueDate").value("2026-04-10"))
			.andExpect(jsonPath("$.data.operationalStatus").value("UPCOMING"));
	}

	@Test
	void deve_atualizar_conta_fixa() throws Exception {
		when(fixedBillService.update(any(Long.class), any(UpdateFixedBillRequest.class))).thenReturn(
			new FixedBillResponse(
				15L,
				"Internet fibra premium",
				new BigDecimal("159.90"),
				LocalDate.of(2026, 4, 10),
				FixedBillFrequency.MONTHLY,
				new ReferenceResponse(10L, "Casa"),
				new ReferenceResponse(20L, "Internet"),
				new ReferenceResponse(30L, "Apartamento Centro"),
				true,
				Instant.parse("2026-03-28T12:00:00Z"),
				LocalDate.of(2026, 4, 10),
				FixedBillOperationalStatus.UPCOMING,
				null
			)
		);

		mockMvc.perform(patch("/api/v1/fixed-bills/15")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"Internet fibra premium",
					  "amount":159.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"MONTHLY",
					  "categoryId":10,
					  "subcategoryId":20,
					  "spaceReferenceId":30
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.description").value("Internet fibra premium"));
	}

	@Test
	void deve_excluir_conta_fixa() throws Exception {
		doNothing().when(fixedBillService).delete(15L);

		mockMvc.perform(delete("/api/v1/fixed-bills/15"))
			.andExpect(status().isNoContent());
	}

	@Test
	void deve_lancar_proxima_despesa_da_conta_fixa() throws Exception {
		when(fixedBillService.launchExpense(15L)).thenReturn(
			new ExpenseResponse(
				90L,
				"Internet fibra",
				new BigDecimal("129.90"),
				LocalDate.of(2026, 4, 10),
				LocalDate.of(2026, 4, 10),
				new ReferenceResponse(10L, "Casa"),
				new ReferenceResponse(20L, "Internet"),
				new ReferenceResponse(30L, "Apartamento Centro"),
				null,
				ExpenseStatus.PREVISTA,
				BigDecimal.ZERO,
				new BigDecimal("129.90"),
				0,
				false,
				Instant.parse("2026-04-01T12:00:00Z"),
				Instant.parse("2026-04-01T12:00:00Z")
			)
		);

		mockMvc.perform(post("/api/v1/fixed-bills/15/launch-expense"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(90))
			.andExpect(jsonPath("$.data.status").value("PREVISTA"));
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

	private FixedBillResponse fixedBillResponse() {
		return new FixedBillResponse(
			15L,
			"Internet fibra",
			new BigDecimal("129.90"),
			LocalDate.of(2026, 4, 10),
			FixedBillFrequency.MONTHLY,
			new ReferenceResponse(10L, "Casa"),
			new ReferenceResponse(20L, "Internet"),
			new ReferenceResponse(30L, "Apartamento Centro"),
			true,
			Instant.parse("2026-03-28T12:00:00Z"),
			LocalDate.of(2026, 4, 10),
			FixedBillOperationalStatus.UPCOMING,
			new FixedBillGeneratedExpenseResponse(
				90L,
				LocalDate.of(2026, 3, 10),
				Instant.parse("2026-03-10T12:00:00Z")
			)
		);
	}
}
