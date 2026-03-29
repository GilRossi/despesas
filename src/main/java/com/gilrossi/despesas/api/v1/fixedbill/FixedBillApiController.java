package com.gilrossi.despesas.api.v1.fixedbill;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.fixedbill.CreateFixedBillRequest;
import com.gilrossi.despesas.fixedbill.FixedBillResponse;
import com.gilrossi.despesas.fixedbill.FixedBillService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fixed-bills")
public class FixedBillApiController {

	private final FixedBillService fixedBillService;

	public FixedBillApiController(FixedBillService fixedBillService) {
		this.fixedBillService = fixedBillService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<FixedBillResponse>> create(@Valid @RequestBody CreateFixedBillRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(fixedBillService.create(request)));
	}
}
