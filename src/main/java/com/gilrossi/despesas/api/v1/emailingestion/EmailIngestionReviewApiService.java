package com.gilrossi.despesas.api.v1.emailingestion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.emailingestion.EmailIngestionRecord;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewService;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class EmailIngestionReviewApiService {

	private final EmailIngestionReviewService reviewService;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public EmailIngestionReviewApiService(
		EmailIngestionReviewService reviewService,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.reviewService = reviewService;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public PageResponse<EmailIngestionReviewSummaryResponse> list(EmailIngestionReviewStatus status, int page, int size) {
		if (status != EmailIngestionReviewStatus.PENDING) {
			throw new IllegalArgumentException("Unsupported review status: " + status);
		}
		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, size);
		List<EmailIngestionRecord> records = reviewService.listPending(currentHouseholdProvider.requireHouseholdId());
		int fromIndex = Math.min(safePage * safeSize, records.size());
		int toIndex = Math.min(fromIndex + safeSize, records.size());
		List<EmailIngestionReviewSummaryResponse> content = records.subList(fromIndex, toIndex).stream()
			.map(EmailIngestionReviewSummaryResponse::from)
			.toList();
		int totalPages = records.isEmpty() ? 0 : (int) Math.ceil((double) records.size() / safeSize);

		return new PageResponse<>(
			content,
			new PageInfo(
				safePage,
				safeSize,
				records.size(),
				totalPages,
				safePage + 1 < totalPages,
				safePage > 0 && totalPages > 0
			)
		);
	}

	@Transactional(readOnly = true)
	public EmailIngestionReviewDetailResponse detail(Long ingestionId) {
		return EmailIngestionReviewDetailResponse.from(
			reviewService.detailPending(currentHouseholdProvider.requireHouseholdId(), ingestionId)
		);
	}

	@Transactional
	public EmailIngestionReviewActionResponse approve(Long ingestionId) {
		return EmailIngestionReviewActionResponse.from(
			reviewService.approve(currentHouseholdProvider.requireHouseholdId(), ingestionId)
		);
	}

	@Transactional
	public EmailIngestionReviewActionResponse reject(Long ingestionId) {
		return EmailIngestionReviewActionResponse.from(
			reviewService.reject(currentHouseholdProvider.requireHouseholdId(), ingestionId)
		);
	}
}
