package com.gilrossi.despesas.historyimport;

import java.util.List;

import com.gilrossi.despesas.payment.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateHistoryImportRequest(
	@NotEmpty(message = "entries must not be empty")
	@Valid
	List<HistoryImportEntryRequest> entries,
	@NotNull(message = "paymentMethod must not be null")
	PaymentMethod paymentMethod
) {
}
