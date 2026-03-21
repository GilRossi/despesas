package com.gilrossi.despesas.api.v1.operations;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.emailingestion.EmailIngestionItem;
import com.gilrossi.despesas.emailingestion.EmailIngestionService;
import com.gilrossi.despesas.emailingestion.ProcessEmailIngestionCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/operations/email-ingestions")
public class OperationalEmailIngestionController {

	private final EmailIngestionService service;

	public OperationalEmailIngestionController(EmailIngestionService service) {
		this.service = service;
	}

	@PostMapping
	public ApiResponse<OperationalEmailIngestionResponse> ingest(@Valid @RequestBody OperationalEmailIngestionRequest request) {
		return new ApiResponse<>(OperationalEmailIngestionResponse.from(service.process(new ProcessEmailIngestionCommand(
			request.sourceAccount(),
			request.externalMessageId(),
			request.sender(),
			request.subject(),
			request.receivedAt(),
			request.merchantOrPayee(),
			request.suggestedCategoryName(),
			request.suggestedSubcategoryName(),
			request.totalAmount(),
			request.dueDate(),
			request.occurredOn(),
			request.currency(),
			request.items() == null ? java.util.List.of() : request.items().stream()
				.map(item -> new EmailIngestionItem(item.description(), item.amount(), item.quantity()))
				.toList(),
			request.summary(),
			request.classification(),
			request.confidence(),
			request.rawReference(),
			request.desiredDecision()
		))));
	}
}
