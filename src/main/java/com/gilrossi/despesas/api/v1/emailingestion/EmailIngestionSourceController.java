package com.gilrossi.despesas.api.v1.emailingestion;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.emailingestion.EmailIngestionSource;
import com.gilrossi.despesas.emailingestion.EmailIngestionSourceService;
import com.gilrossi.despesas.emailingestion.RegisterEmailIngestionSourceCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/email-ingestion/sources")
@PreAuthorize("hasRole('OWNER')")
public class EmailIngestionSourceController {

	private final EmailIngestionSourceService service;

	public EmailIngestionSourceController(EmailIngestionSourceService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<EmailIngestionSourceResponse>> list() {
		return new ApiResponse<>(service.list().stream()
			.map(EmailIngestionSourceResponse::from)
			.toList());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<EmailIngestionSourceResponse>> create(@Valid @RequestBody EmailIngestionSourceRequest request) {
		EmailIngestionSource source = service.register(new RegisterEmailIngestionSourceCommand(
			request.sourceAccount(),
			request.label(),
			request.autoImportMinConfidence(),
			request.reviewMinConfidence()
		));
		return ResponseEntity.status(201).body(new ApiResponse<>(EmailIngestionSourceResponse.from(source)));
	}
}
