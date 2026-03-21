package com.gilrossi.despesas.emailingestion;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.expense.ExpenseResponse;

@Service
public class EmailIngestionReviewService {

	private static final String SUPPORTED_CURRENCY = "BRL";

	private final EmailIngestionRecordRepository recordRepository;
	private final EmailIngestionExpenseImportService expenseImportService;

	public EmailIngestionReviewService(
		EmailIngestionRecordRepository recordRepository,
		EmailIngestionExpenseImportService expenseImportService
	) {
		this.recordRepository = recordRepository;
		this.expenseImportService = expenseImportService;
	}

	@Transactional(readOnly = true)
	public List<EmailIngestionRecord> listPending(Long householdId) {
		return recordRepository.findAllPendingReviewByHouseholdId(householdId);
	}

	@Transactional
	public EmailIngestionReviewActionResult approve(Long householdId, Long ingestionId) {
		EmailIngestionRecord record = requirePendingRecord(householdId, ingestionId);
		validateImportable(record);
		try {
			ExpenseResponse expense = expenseImportService.importExpense(
				householdId,
				record.sourceAccount(),
				toCommand(record)
			);
			EmailIngestionRecord saved = recordRepository.save(updatedRecord(
				record,
				EmailIngestionFinalDecision.AUTO_IMPORTED,
				EmailIngestionDecisionReason.MANUALLY_IMPORTED,
				expense.id()
			));
			return new EmailIngestionReviewActionResult(saved.id(), saved.finalDecision(), saved.decisionReason(), saved.importedExpenseId());
		} catch (EmailIngestionImportReviewException exception) {
			throw new EmailIngestionReviewActionNotAllowedException(messageForImportReason(exception.getReason()));
		} catch (IllegalArgumentException exception) {
			throw new EmailIngestionReviewActionNotAllowedException("Esta ingestão ainda não pode ser aprovada porque os dados extraídos estão incompletos.");
		}
	}

	@Transactional
	public EmailIngestionReviewActionResult reject(Long householdId, Long ingestionId) {
		EmailIngestionRecord record = requirePendingRecord(householdId, ingestionId);
		EmailIngestionRecord saved = recordRepository.save(updatedRecord(
			record,
			EmailIngestionFinalDecision.IGNORED,
			EmailIngestionDecisionReason.MANUALLY_REJECTED,
			null
		));
		return new EmailIngestionReviewActionResult(saved.id(), saved.finalDecision(), saved.decisionReason(), saved.importedExpenseId());
	}

	private EmailIngestionRecord requirePendingRecord(Long householdId, Long ingestionId) {
		EmailIngestionRecord record = recordRepository.findByIdAndHouseholdId(ingestionId, householdId)
			.orElseThrow(() -> new EmailIngestionReviewNotFoundException(ingestionId));
		if (record.finalDecision() != EmailIngestionFinalDecision.REVIEW_REQUIRED || record.importedExpenseId() != null) {
			throw new EmailIngestionReviewActionNotAllowedException("A ingestão selecionada não está mais pendente de revisão.");
		}
		return record;
	}

	private void validateImportable(EmailIngestionRecord record) {
		if (record.totalAmount() == null) {
			throw new EmailIngestionReviewActionNotAllowedException("Esta ingestão ainda não pode ser aprovada porque o total não foi extraído.");
		}
		if (!SUPPORTED_CURRENCY.equals(record.currency() == null ? null : record.currency().trim().toUpperCase(Locale.ROOT))) {
			throw new EmailIngestionReviewActionNotAllowedException("Esta ingestão ainda não pode ser aprovada porque a moeda não é suportada.");
		}
	}

	private ProcessEmailIngestionCommand toCommand(EmailIngestionRecord record) {
		return new ProcessEmailIngestionCommand(
			record.sourceAccount(),
			record.externalMessageId(),
			record.sender(),
			record.subject(),
			record.receivedAt(),
			record.merchantOrPayee(),
			record.suggestedCategoryName(),
			record.suggestedSubcategoryName(),
			record.totalAmount(),
			record.dueDate(),
			record.occurredOn(),
			record.currency(),
			record.items() == null ? List.of() : record.items(),
			record.summary(),
			record.classification(),
			record.confidence(),
			record.rawReference(),
			record.desiredDecision()
		);
	}

	private EmailIngestionRecord updatedRecord(
		EmailIngestionRecord record,
		EmailIngestionFinalDecision finalDecision,
		EmailIngestionDecisionReason reason,
		Long importedExpenseId
	) {
		return new EmailIngestionRecord(
			record.id(),
			record.householdId(),
			record.sourceId(),
			record.sourceAccount(),
			record.normalizedSourceAccount(),
			record.externalMessageId(),
			record.sender(),
			record.subject(),
			record.receivedAt(),
			record.merchantOrPayee(),
			record.suggestedCategoryName(),
			record.suggestedSubcategoryName(),
			record.totalAmount(),
			record.dueDate(),
			record.occurredOn(),
			record.currency(),
			record.summary(),
			record.classification(),
			record.confidence(),
			record.rawReference(),
			record.desiredDecision(),
			finalDecision,
			reason,
			record.fingerprint(),
			importedExpenseId,
			record.createdAt(),
			record.updatedAt(),
			record.items()
		);
	}

	private String messageForImportReason(EmailIngestionDecisionReason reason) {
		return switch (reason) {
			case CATEGORY_NOT_FOUND -> "Esta ingestão ainda não pode ser aprovada porque a categoria sugerida não está mapeada.";
			case SUBCATEGORY_NOT_FOUND -> "Esta ingestão ainda não pode ser aprovada porque a subcategoria sugerida não está mapeada.";
			case SUBCATEGORY_REQUIRED -> "Esta ingestão ainda não pode ser aprovada porque a subcategoria não pôde ser resolvida.";
			case MISSING_TOTAL_AMOUNT -> "Esta ingestão ainda não pode ser aprovada porque o total não foi extraído.";
			case UNSUPPORTED_CURRENCY -> "Esta ingestão ainda não pode ser aprovada porque a moeda não é suportada.";
			default -> "Esta ingestão ainda não pode ser aprovada com os dados disponíveis.";
		};
	}
}
