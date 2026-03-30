package com.gilrossi.despesas.api.v1.expense;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseDetailResponse;
import com.gilrossi.despesas.expense.ExpenseFilter;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseService;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.UpdateExpenseRequest;

@WebMvcTest(ExpenseApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class ExpenseApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExpenseService expenseService;

	@Test
	void deve_listar_despesas_em_formato_json_paginado() throws Exception {
		ExpenseResponse response = novaExpenseResponse(1L, ExpenseStatus.ABERTA);
		when(expenseService.listar(any(ExpenseFilter.class), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10))).thenReturn(
			new PageResponse<>(List.of(response), new PageInfo(0, 10, 1, 1, false, false))
		);

		mockMvc.perform(get("/api/v1/expenses"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].category.id").value(10))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void deve_repassar_o_filtro_overdue_para_o_service() throws Exception {
		when(expenseService.listar(any(ExpenseFilter.class), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10))).thenReturn(
			new PageResponse<>(List.of(), new PageInfo(0, 10, 0, 0, false, false))
		);

		mockMvc.perform(get("/api/v1/expenses")
				.param("overdue", "true"))
			.andExpect(status().isOk());

		ArgumentCaptor<ExpenseFilter> captor = ArgumentCaptor.forClass(ExpenseFilter.class);
		verify(expenseService).listar(captor.capture(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(10));
		assertTrue(Boolean.TRUE.equals(captor.getValue().overdue()));
	}

	@Test
	void deve_retornar_detalhe_da_despesa() throws Exception {
		when(expenseService.detalhar(10L)).thenReturn(novoDetalhe(10L, ExpenseStatus.PARCIALMENTE_PAGA));

		mockMvc.perform(get("/api/v1/expenses/10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.category.id").value(10))
			.andExpect(jsonPath("$.data.paymentsCount").value(1));
	}

	@Test
	void deve_criar_despesa_e_retornar_status_prevista() throws Exception {
		ExpenseResponse response = novaExpenseResponse(10L, ExpenseStatus.PREVISTA);
		when(expenseService.criar(any(CreateExpenseRequest.class))).thenReturn(response);
		LocalDate occurredOn = LocalDate.now();

		mockMvc.perform(post("/api/v1/expenses")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"description": "Internet da casa",
						"amount": 120.00,
						"occurredOn": "%s",
						"dueDate": "%s",
						"categoryId": 10,
						"subcategoryId": 20,
						"notes": "Conta fixa"
					}
					""".formatted(occurredOn, occurredOn.plusDays(5))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.status").value("PREVISTA"));
	}

	@Test
	void deve_rejeitar_payload_invalido_quando_descricao_estiver_vazia() throws Exception {
		LocalDate occurredOn = LocalDate.now();
		mockMvc.perform(post("/api/v1/expenses")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"description": "",
						"amount": 120.00,
						"occurredOn": "%s",
						"dueDate": "%s",
						"categoryId": 10,
						"subcategoryId": 20
					}
					""".formatted(occurredOn, occurredOn.plusDays(5))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("Request validation failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
	}

	@Test
	void deve_atualizar_despesa_e_retornar_o_novo_estado() throws Exception {
		when(expenseService.atualizar(any(), any(UpdateExpenseRequest.class))).thenReturn(novoDetalhe(10L, ExpenseStatus.PREVISTA));
		LocalDate occurredOn = LocalDate.now();

		mockMvc.perform(patch("/api/v1/expenses/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"description": "Internet da casa",
						"amount": 150.00,
						"occurredOn": "%s",
						"dueDate": "%s",
						"categoryId": 10,
						"subcategoryId": 20,
						"notes": "Atualizada"
					}
					""".formatted(occurredOn, occurredOn.plusDays(3))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.amount").value(150.00));
	}

	@Test
	void deve_excluir_despesa() throws Exception {
		mockMvc.perform(delete("/api/v1/expenses/10"))
			.andExpect(status().isNoContent());

		verify(expenseService).deletar(10L);
	}

	private ExpenseResponse novaExpenseResponse(Long id, ExpenseStatus status) {
		LocalDate occurredOn = LocalDate.now();
		return new ExpenseResponse(
			id,
			"Internet da casa",
			new BigDecimal("120.00"),
			occurredOn.plusDays(5),
			occurredOn,
			new ReferenceResponse(10L, "Moradia"),
			new ReferenceResponse(20L, "Internet"),
			null,
			"Conta fixa",
			status,
			new BigDecimal("0.00"),
			new BigDecimal("120.00"),
			0,
			false,
			null,
			null
		);
	}

	private ExpenseDetailResponse novoDetalhe(Long id, ExpenseStatus status) {
		LocalDate occurredOn = LocalDate.now();
		return new ExpenseDetailResponse(
			id,
			"Internet da casa",
			new BigDecimal("150.00"),
			occurredOn.plusDays(3),
			occurredOn,
			new ReferenceResponse(10L, "Moradia"),
			new ReferenceResponse(20L, "Internet"),
			null,
			"Atualizada",
			status,
			new BigDecimal("40.00"),
			new BigDecimal("110.00"),
			1,
			false,
			null,
			null,
			List.of()
		);
	}
}
