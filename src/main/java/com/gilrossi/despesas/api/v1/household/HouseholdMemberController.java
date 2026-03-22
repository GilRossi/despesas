package com.gilrossi.despesas.api.v1.household;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.identity.CreateHouseholdMemberCommand;
import com.gilrossi.despesas.identity.HouseholdMemberResponse;
import com.gilrossi.despesas.identity.HouseholdMemberService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/household/members")
public class HouseholdMemberController {

	private final HouseholdMemberService service;

	public HouseholdMemberController(HouseholdMemberService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<HouseholdMemberResponse>> listar() {
		return new ApiResponse<>(service.listMembers());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<HouseholdMemberResponse>> criar(@Valid @RequestBody CreateHouseholdMemberRequest request) {
		HouseholdMemberResponse response = service.create(new CreateHouseholdMemberCommand(
			request.name(),
			request.email(),
			request.password(),
			null
		));
		return ResponseEntity.status(201).body(new ApiResponse<>(response));
	}
}
