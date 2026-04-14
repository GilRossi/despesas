package com.gilrossi.despesas.api.v1.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/spaces")
public class PlatformAdminSpaceController {

	private final PlatformAdminSpaceService service;

	public PlatformAdminSpaceController(PlatformAdminSpaceService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PlatformAdminSpaceResponse>> create(@Valid @RequestBody CreatePlatformSpaceRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(service.create(request)));
	}

	@GetMapping
	public ApiResponse<List<PlatformAdminSpaceResponse>> list() {
		return new ApiResponse<>(service.list());
	}

	@GetMapping("/{spaceId}")
	public ApiResponse<PlatformAdminSpaceResponse> read(@PathVariable Long spaceId) {
		return new ApiResponse<>(service.read(spaceId));
	}

	@PutMapping("/{spaceId}/modules")
	public ApiResponse<PlatformAdminSpaceResponse> updateModules(
		@PathVariable Long spaceId,
		@RequestBody UpdateSpaceModulesRequest request
	) {
		return new ApiResponse<>(service.updateModules(spaceId, request));
	}
}
