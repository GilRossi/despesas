package com.gilrossi.despesas.api.v1.expense;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseDetailResponse;
import com.gilrossi.despesas.expense.ExpenseFilter;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseService;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.UpdateExpenseRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseApiController {

	private final ExpenseService expenseService;

	public ExpenseApiController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	@GetMapping
	public PageResponse<ExpenseResponse> listar(
		@RequestParam(required = false) String q,
		@RequestParam(required = false) ExpenseContext context,
		@RequestParam(required = false) Long categoryId,
		@RequestParam(required = false) Long subcategoryId,
		@RequestParam(required = false) ExpenseStatus status,
		@RequestParam(required = false) Boolean overdue,
		@RequestParam(required = false) LocalDate dueDateFrom,
		@RequestParam(required = false) LocalDate dueDateTo,
		@RequestParam(required = false) Boolean hasPayments,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return expenseService.listar(
			new ExpenseFilter(q, context, categoryId, subcategoryId, status, overdue, dueDateFrom, dueDateTo, hasPayments),
			page,
			size
		);
	}

	@GetMapping("/{id}")
	public ApiResponse<ExpenseDetailResponse> detalhar(@PathVariable Long id) {
		return new ApiResponse<>(expenseService.detalhar(id));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ExpenseResponse>> criar(@Valid @RequestBody CreateExpenseRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(expenseService.criar(request)));
	}

	@PatchMapping("/{id}")
	public ApiResponse<ExpenseDetailResponse> atualizar(@PathVariable Long id, @Valid @RequestBody UpdateExpenseRequest request) {
		return new ApiResponse<>(expenseService.atualizar(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		expenseService.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
