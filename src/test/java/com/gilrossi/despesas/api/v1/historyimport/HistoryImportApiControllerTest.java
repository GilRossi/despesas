package com.gilrossi.despesas.api.v1.historyimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.gilrossi.despesas.api.v1.shared.FieldErrorResponse;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.historyimport.CreateHistoryImportRequest;
import com.gilrossi.despesas.historyimport.HistoryImportEntryResponse;
import com.gilrossi.despesas.historyimport.HistoryImportResponse;
import com.gilrossi.despesas.historyimport.HistoryImportService;

@WebMvcTest(HistoryImportApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class HistoryImportApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HistoryImportService historyImportService;

	@Test
	void deve_importar_lote_e_retornar_resposta_minima() throws Exception {
		when(historyImportService.importHistory(any(CreateHistoryImportRequest.class))).thenReturn(new HistoryImportResponse(
			2,
			List.of(
				new HistoryImportEntryResponse(
					11L,
					101L,
					"Internet fevereiro",
					new BigDecimal("129.90"),
					LocalDate.of(2026, 2, 10),
					ExpenseStatus.PAGA
				),
				new HistoryImportEntryResponse(
					12L,
					102L,
					"Aluguel fevereiro",
					new BigDecimal("1500.00"),
					LocalDate.of(2026, 2, 5),
					ExpenseStatus.PAGA
				)
			)
		));

		mockMvc.perform(post("/api/v1/history-imports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Internet fevereiro",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":10,
					      "subcategoryId":20
					    },
					    {
					      "description":"Aluguel fevereiro",
					      "amount":1500.00,
					      "date":"2026-02-05",
					      "categoryId":10,
					      "subcategoryId":21
					    }
					  ]
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.importedCount").value(2))
			.andExpect(jsonPath("$.data.entries[0].expenseId").value(11))
			.andExpect(jsonPath("$.data.entries[0].paymentId").value(101))
			.andExpect(jsonPath("$.data.entries[0].description").value("Internet fevereiro"))
			.andExpect(jsonPath("$.data.entries[0].amount").value(129.90))
			.andExpect(jsonPath("$.data.entries[0].date").value("2026-02-10"))
			.andExpect(jsonPath("$.data.entries[0].status").value("PAGA"));
	}

	@Test
	void deve_rejeitar_lote_vazio() throws Exception {
		mockMvc.perform(post("/api/v1/history-imports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[]
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries"));
	}

	@Test
	void deve_rejeitar_item_com_descricao_vazia() throws Exception {
		mockMvc.perform(post("/api/v1/history-imports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":10,
					      "subcategoryId":20
					    }
					  ]
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries[0].description"));
	}

	@Test
	void deve_rejeitar_payment_method_ausente() throws Exception {
		mockMvc.perform(post("/api/v1/history-imports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "entries":[
					    {
					      "description":"Internet fevereiro",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":10,
					      "subcategoryId":20
					    }
					  ]
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("paymentMethod"));
	}

	@Test
	void deve_retornar_field_errors_indexados_quando_regra_de_negocio_do_lote_falhar() throws Exception {
		when(historyImportService.importHistory(any(CreateHistoryImportRequest.class))).thenThrow(
			new FieldBusinessRuleException(
				"History import validation failed",
				List.of(
					new FieldErrorResponse(
						"entries[1].subcategoryId",
						"subcategoryId must belong to the informed category"
					)
				)
			)
		);

		mockMvc.perform(post("/api/v1/history-imports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Internet fevereiro",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":10,
					      "subcategoryId":20
					    },
					    {
					      "description":"Historico invalido",
					      "amount":89.90,
					      "date":"2026-02-11",
					      "categoryId":10,
					      "subcategoryId":999
					    }
					  ]
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.message").value("History import validation failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries[1].subcategoryId"));
	}
}
