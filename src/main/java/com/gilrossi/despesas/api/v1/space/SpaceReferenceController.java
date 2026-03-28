package com.gilrossi.despesas.api.v1.space;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.spacereference.CreateSpaceReferenceCommand;
import com.gilrossi.despesas.spacereference.SpaceReference;
import com.gilrossi.despesas.spacereference.SpaceReferenceCreateResult;
import com.gilrossi.despesas.spacereference.SpaceReferenceService;
import com.gilrossi.despesas.spacereference.SpaceReferenceType;
import com.gilrossi.despesas.spacereference.SpaceReferenceTypeGroup;

@RestController
@RequestMapping("/api/v1/space/references")
public class SpaceReferenceController {

	private final SpaceReferenceService service;

	public SpaceReferenceController(SpaceReferenceService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<SpaceReferenceResponse>> list(
		@RequestParam(required = false) SpaceReferenceTypeGroup typeGroup,
		@RequestParam(required = false) SpaceReferenceType type,
		@RequestParam(required = false) String q
	) {
		List<SpaceReference> references = service.list(typeGroup, type, q);
		return new ApiResponse<>(references.stream().map(SpaceReferenceResponse::from).toList());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<SpaceReferenceCreateResponse>> create(@Valid @RequestBody CreateSpaceReferenceRequest request) {
		SpaceReferenceCreateResult result = service.create(new CreateSpaceReferenceCommand(request.type(), request.name()));
		SpaceReferenceCreateResponse response = SpaceReferenceCreateResponse.from(result);
		if (result.isCreated()) {
			return ResponseEntity.status(201).body(new ApiResponse<>(response));
		}
		return ResponseEntity.ok(new ApiResponse<>(response));
	}
}
