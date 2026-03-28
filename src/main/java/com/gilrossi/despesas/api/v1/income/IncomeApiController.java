package com.gilrossi.despesas.api.v1.income;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.income.CreateIncomeRequest;
import com.gilrossi.despesas.income.IncomeResponse;
import com.gilrossi.despesas.income.IncomeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/incomes")
public class IncomeApiController {

	private final IncomeService incomeService;

	public IncomeApiController(IncomeService incomeService) {
		this.incomeService = incomeService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<IncomeResponse>> create(@Valid @RequestBody CreateIncomeRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(incomeService.create(request)));
	}
}
