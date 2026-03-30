package com.gilrossi.despesas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.catalog.CatalogOptionsResponse;
import com.gilrossi.despesas.catalog.CatalogQueryService;
import com.gilrossi.despesas.catalog.CatalogSubcategoryOption;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseDetailResponse;
import com.gilrossi.despesas.expense.ExpenseFilter;
import com.gilrossi.despesas.expense.ExpenseNotFoundException;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseService;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.UpdateExpenseRequest;
import com.gilrossi.despesas.model.Despesa;

@ExtendWith(MockitoExtension.class)
class DespesaServiceTest {

	@Mock
	private ExpenseService expenseService;

	@Mock
	private CatalogQueryService catalogQueryService;

	private DespesaService service;

	@BeforeEach
	void setUp() {
		service = new DespesaService(expenseService, catalogQueryService);
	}

	@Test
	void deve_listar_despesas_convertendo_paginacao_do_dominio_novo() {
		when(expenseService.listar(any(ExpenseFilter.class), eq(0), eq(20))).thenReturn(new PageResponse<>(
			List.of(novaExpenseResponse(1L, "Moradia", "Internet")),
			new PageInfo(0, 20, 1, 1, false, false)
		));

		Page<Despesa> pagina = service.listarDespesas(-1, 99);

		assertEquals(1, pagina.getTotalElements());
		assertEquals(1, pagina.getContent().size());
		assertEquals("Internet da casa", pagina.getContent().getFirst().getDescricao());
		assertEquals("Moradia", pagina.getContent().getFirst().getCategoria());
		assertEquals("Internet", pagina.getContent().getFirst().getSubcategoria());
	}

	@Test
	void deve_salvar_nova_despesa_via_expense_service() {
		service.salvar(novaDespesa());

		ArgumentCaptor<CreateExpenseRequest> captor = ArgumentCaptor.forClass(CreateExpenseRequest.class);
		verify(expenseService).criar(captor.capture());
		assertEquals("Internet da casa", captor.getValue().description());
		assertEquals(new BigDecimal("120.00"), captor.getValue().amount());
		assertEquals(10L, captor.getValue().categoryId());
		assertEquals(20L, captor.getValue().subcategoryId());
	}

	@Test
	void deve_atualizar_despesa_existente_via_expense_service() {
		Despesa despesa = novaDespesa();
		despesa.setId(11L);

		service.salvar(despesa);

		ArgumentCaptor<UpdateExpenseRequest> captor = ArgumentCaptor.forClass(UpdateExpenseRequest.class);
		verify(expenseService).atualizar(eq(11L), captor.capture());
		assertEquals("Internet da casa", captor.getValue().description());
		assertEquals(new BigDecimal("120.00"), captor.getValue().amount());
	}

	@Test
	void deve_converter_detalhe_da_despesa_para_formulario_web() {
		when(expenseService.detalhar(7L)).thenReturn(new ExpenseDetailResponse(
			7L,
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.of(2026, 3, 20),
			LocalDate.of(2026, 3, 18),
			new ReferenceResponse(10L, "Moradia"),
			new ReferenceResponse(20L, "Internet"),
			null,
			"Conta fixa",
			ExpenseStatus.ABERTA,
			BigDecimal.ZERO,
			new BigDecimal("120.00"),
			0,
			false,
			null,
			null,
			List.of()
		));

		Despesa despesa = service.buscaPorId(7L);

		assertEquals(7L, despesa.getId());
		assertEquals("Moradia", despesa.getCategoria());
		assertEquals("Internet", despesa.getSubcategoria());
		assertEquals(10L, despesa.getCategoriaId());
		assertEquals(20L, despesa.getSubcategoriaId());
	}

	@Test
	void deve_repassar_catalogo_ativo_para_o_mvc() {
		when(catalogQueryService.listarOpcoesAtivas()).thenReturn(List.of(
			new CatalogOptionsResponse(10L, "Moradia", List.of(new CatalogSubcategoryOption(20L, "Internet")))
		));

		List<CatalogOptionsResponse> catalogo = service.listarCatalogo();

		assertEquals(1, catalogo.size());
		assertEquals("Moradia", catalogo.getFirst().name());
	}

	@Test
	void deve_lancar_excecao_web_quando_despesa_nao_existir() {
		when(expenseService.detalhar(99L)).thenThrow(new ExpenseNotFoundException(99L));

		assertThrows(DespesaNotFoundException.class, () -> service.buscaPorId(99L));
	}

	@Test
	void deve_delegar_exclusao_para_o_dominio_novo() {
		service.deletar(15L);

		verify(expenseService).deletar(15L);
	}

	private Despesa novaDespesa() {
		Despesa despesa = new Despesa();
		despesa.setDescricao("Internet da casa");
		despesa.setValor(new BigDecimal("120.00"));
		despesa.setData(LocalDate.of(2026, 3, 20));
		despesa.setContexto(ExpenseContext.CASA);
		despesa.setCategoriaId(10L);
		despesa.setCategoria("Moradia");
		despesa.setSubcategoriaId(20L);
		despesa.setSubcategoria("Internet");
		despesa.setObservacoes("Conta fixa");
		return despesa;
	}

	private ExpenseResponse novaExpenseResponse(Long id, String categoria, String subcategoria) {
		return new ExpenseResponse(
			id,
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.of(2026, 3, 20),
			LocalDate.of(2026, 3, 18),
			new ReferenceResponse(10L, categoria),
			new ReferenceResponse(20L, subcategoria),
			null,
			"Conta fixa",
			ExpenseStatus.ABERTA,
			BigDecimal.ZERO,
			new BigDecimal("120.00"),
			0,
			false,
			null,
			null
		);
	}
}
