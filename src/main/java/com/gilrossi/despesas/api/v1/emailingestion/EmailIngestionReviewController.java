package com.gilrossi.despesas.api.v1.emailingestion;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.api.v1.shared.PageResponse;

@RestController
@RequestMapping("/api/v1/email-ingestion/reviews")
@PreAuthorize("hasRole('OWNER')")
public class EmailIngestionReviewController {

	private final EmailIngestionReviewApiService service;

	public EmailIngestionReviewController(EmailIngestionReviewApiService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<EmailIngestionReviewSummaryResponse> list(
		@RequestParam(defaultValue = "PENDING") EmailIngestionReviewStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return service.list(status, page, size);
	}

	@GetMapping("/{ingestionId}")
	public ApiResponse<EmailIngestionReviewDetailResponse> detail(@PathVariable Long ingestionId) {
		return new ApiResponse<>(service.detail(ingestionId));
	}

	@PostMapping("/{ingestionId}/approve")
	public ApiResponse<EmailIngestionReviewActionResponse> approve(@PathVariable Long ingestionId) {
		return new ApiResponse<>(service.approve(ingestionId));
	}

	@PostMapping("/{ingestionId}/reject")
	public ApiResponse<EmailIngestionReviewActionResponse> reject(@PathVariable Long ingestionId) {
		return new ApiResponse<>(service.reject(ingestionId));
	}
}
