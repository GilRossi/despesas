package com.gilrossi.despesas.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.catalog.CatalogOptionsResponse;
import com.gilrossi.despesas.catalog.CatalogQueryService;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseDetailResponse;
import com.gilrossi.despesas.expense.ExpenseFilter;
import com.gilrossi.despesas.expense.ExpenseNotFoundException;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseService;
import com.gilrossi.despesas.expense.UpdateExpenseRequest;
import com.gilrossi.despesas.model.Despesa;

@Service
public class DespesaService {

	private static final int TAMANHO_MAXIMO_PAGINA = 20;

	private final ExpenseService expenseService;
	private final CatalogQueryService catalogQueryService;

	public DespesaService(ExpenseService expenseService, CatalogQueryService catalogQueryService) {
		this.expenseService = expenseService;
		this.catalogQueryService = catalogQueryService;
	}

	@Transactional(readOnly = true)
	public Page<Despesa> listarDespesas(int pagina, int tamanho) {
		int paginaNormalizada = Math.max(pagina, 0);
		int tamanhoNormalizado = Math.max(1, Math.min(tamanho, TAMANHO_MAXIMO_PAGINA));
		PageResponse<ExpenseResponse> response = expenseService.listar(
			new ExpenseFilter(null, null, null, null, null, null, null, null, null),
			paginaNormalizada,
			tamanhoNormalizado
		);
		List<Despesa> content = response.content().stream()
			.map(this::toView)
			.toList();
		return new PageImpl<>(content, org.springframework.data.domain.PageRequest.of(
			response.page().page(),
			response.page().size()
		), response.page().totalElements());
	}

	@Transactional(readOnly = true)
	public Despesa buscaPorId(Long id) {
		try {
			return toForm(expenseService.detalhar(id));
		} catch (ExpenseNotFoundException exception) {
			throw new DespesaNotFoundException(id);
		}
	}

	@Transactional(readOnly = true)
	public List<CatalogOptionsResponse> listarCatalogo() {
		return catalogQueryService.listarOpcoesAtivas();
	}

	@Transactional
	public void salvar(Despesa despesa) {
		try {
			if (despesa.getId() == null) {
				expenseService.criar(toCreateRequest(despesa));
				return;
			}
			expenseService.atualizar(despesa.getId(), toUpdateRequest(despesa));
		} catch (ExpenseNotFoundException exception) {
			throw new DespesaNotFoundException(despesa.getId());
		}
	}

	@Transactional
	public void deletar(Long id) {
		try {
			expenseService.deletar(id);
		} catch (ExpenseNotFoundException exception) {
			throw new DespesaNotFoundException(id);
		}
	}

	private Despesa toView(ExpenseResponse response) {
		Despesa despesa = new Despesa();
		despesa.setId(response.id());
		despesa.setDescricao(response.description());
		despesa.setValor(response.amount());
		despesa.setData(response.dueDate());
		despesa.setContexto(response.context());
		despesa.setCategoriaId(response.category().id());
		despesa.setCategoria(response.category().name());
		despesa.setSubcategoriaId(response.subcategory().id());
		despesa.setSubcategoria(response.subcategory().name());
		despesa.setObservacoes(response.notes());
		despesa.setStatus(response.status());
		return despesa;
	}

	private Despesa toForm(ExpenseDetailResponse response) {
		Despesa despesa = toView(new ExpenseResponse(
			response.id(),
			response.description(),
			response.amount(),
			response.dueDate(),
			response.context(),
			response.category(),
			response.subcategory(),
			response.notes(),
			response.status(),
			response.paidAmount(),
			response.remainingAmount(),
			response.paymentsCount(),
			response.overdue(),
			response.createdAt(),
			response.updatedAt()
		));
		despesa.setObservacoes(response.notes());
		return despesa;
	}

	private CreateExpenseRequest toCreateRequest(Despesa despesa) {
		return new CreateExpenseRequest(
			despesa.getDescricao(),
			despesa.getValor(),
			despesa.getData(),
			despesa.getContexto(),
			despesa.getCategoriaId(),
			despesa.getSubcategoriaId(),
			despesa.getObservacoes()
		);
	}

	private UpdateExpenseRequest toUpdateRequest(Despesa despesa) {
		return new UpdateExpenseRequest(
			despesa.getDescricao(),
			despesa.getValor(),
			despesa.getData(),
			despesa.getContexto(),
			despesa.getCategoriaId(),
			despesa.getSubcategoriaId(),
			despesa.getObservacoes()
		);
	}
}
