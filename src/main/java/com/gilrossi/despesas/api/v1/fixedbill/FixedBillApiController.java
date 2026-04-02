package com.gilrossi.despesas.api.v1.fixedbill;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.fixedbill.CreateFixedBillRequest;
import com.gilrossi.despesas.fixedbill.FixedBillResponse;
import com.gilrossi.despesas.fixedbill.FixedBillService;
import com.gilrossi.despesas.fixedbill.UpdateFixedBillRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fixed-bills")
public class FixedBillApiController {

	private final FixedBillService fixedBillService;

	public FixedBillApiController(FixedBillService fixedBillService) {
		this.fixedBillService = fixedBillService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<java.util.List<FixedBillResponse>>> listActive() {
		return ResponseEntity.ok(new ApiResponse<>(fixedBillService.listActive()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<FixedBillResponse>> detail(@PathVariable Long id) {
		return ResponseEntity.ok(new ApiResponse<>(fixedBillService.detail(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<FixedBillResponse>> create(@Valid @RequestBody CreateFixedBillRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(fixedBillService.create(request)));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<ApiResponse<FixedBillResponse>> update(@PathVariable Long id, @Valid @RequestBody UpdateFixedBillRequest request) {
		return ResponseEntity.ok(new ApiResponse<>(fixedBillService.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		fixedBillService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/launch-expense")
	public ResponseEntity<ApiResponse<ExpenseResponse>> launchExpense(@PathVariable Long id) {
		return ResponseEntity.status(201).body(new ApiResponse<>(fixedBillService.launchExpense(id)));
	}
}
