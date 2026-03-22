package com.gilrossi.despesas.api.v1.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/households")
public class PlatformAdminHouseholdController {

	private final PlatformAdminHouseholdProvisioningService service;

	public PlatformAdminHouseholdController(PlatformAdminHouseholdProvisioningService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<HouseholdOwnerProvisioningResponse>> create(@Valid @RequestBody CreateHouseholdOwnerRequest request) {
		HouseholdOwnerProvisioningResponse response = service.createHouseholdWithOwner(request);
		return ResponseEntity.status(201).body(new ApiResponse<>(response));
	}
}
