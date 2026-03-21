package com.gilrossi.despesas.api.v1.emailingestion;

import java.math.BigDecimal;

import com.gilrossi.despesas.emailingestion.EmailIngestionItem;

public record EmailIngestionReviewItemResponse(
	String description,
	BigDecimal amount,
	BigDecimal quantity
) {

	public static EmailIngestionReviewItemResponse from(EmailIngestionItem item) {
		return new EmailIngestionReviewItemResponse(item.description(), item.amount(), item.quantity());
	}
}
