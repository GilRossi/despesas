package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.security.OperationalEmailIngestionAuditLogger;

@Service
public class EmailIngestionService {

	private static final String SUPPORTED_CURRENCY = "BRL";

	private final EmailIngestionSourceRepository sourceRepository;
	private final EmailIngestionRecordRepository recordRepository;
	private final EmailIngestionFingerprintFactory fingerprintFactory;
	private final EmailIngestionExpenseImportService expenseImportService;
	private final TransactionOperations transactionOperations;
	private final OperationalEmailIngestionAuditLogger auditLogger;

	@Autowired
	public EmailIngestionService(
		EmailIngestionSourceRepository sourceRepository,
		EmailIngestionRecordRepository recordRepository,
		EmailIngestionFingerprintFactory fingerprintFactory,
		EmailIngestionExpenseImportService expenseImportService,
		PlatformTransactionManager transactionManager,
		OperationalEmailIngestionAuditLogger auditLogger
	) {
		this(
			sourceRepository,
			recordRepository,
			fingerprintFactory,
			expenseImportService,
			new TransactionTemplate(transactionManager),
			auditLogger
		);
	}

	EmailIngestionService(
		EmailIngestionSourceRepository sourceRepository,
		EmailIngestionRecordRepository recordRepository,
		EmailIngestionFingerprintFactory fingerprintFactory,
		EmailIngestionExpenseImportService expenseImportService,
		TransactionOperations transactionOperations,
		OperationalEmailIngestionAuditLogger auditLogger
	) {
		this.sourceRepository = sourceRepository;
		this.recordRepository = recordRepository;
		this.fingerprintFactory = fingerprintFactory;
		this.expenseImportService = expenseImportService;
		this.transactionOperations = transactionOperations;
		this.auditLogger = auditLogger;
	}

	public EmailIngestionResult process(ProcessEmailIngestionCommand command) {
		String normalizedSourceAccount = normalizeRequired(command.sourceAccount(), "sourceAccount").toLowerCase(Locale.ROOT);
		EmailIngestionSource source = sourceRepository.findActiveByNormalizedSourceAccount(normalizedSourceAccount)
			.orElseThrow(() -> unmappedSourceAccount(normalizedSourceAccount));
		ProcessEmailIngestionCommand normalizedCommand = normalizeCommand(command, normalizedSourceAccount);
		EmailIngestionResult result = recordRepository.findBySourceAccountAndExternalMessageId(normalizedSourceAccount, normalizedCommand.externalMessageId())
			.map(existing -> duplicateResult(existing, EmailIngestionDecisionReason.DUPLICATE_MESSAGE_ID))
			.orElseGet(() -> processUniqueCandidateSafely(source, normalizedCommand));
		auditLogger.ingestionAccepted(
			source.sourceAccount(),
			result.householdId(),
			result.ingestionId(),
			result.decision(),
			result.duplicate()
		);
		return result;
	}

	private EmailIngestionResult processUniqueCandidateSafely(EmailIngestionSource source, ProcessEmailIngestionCommand command) {
		try {
			return transactionOperations.execute(status -> processUniqueCandidate(source, command));
		} catch (DataIntegrityViolationException exception) {
			EmailIngestionRecord duplicateByMessageId = recordRepository.findBySourceAccountAndExternalMessageId(
				source.normalizedSourceAccount(),
				command.externalMessageId()
			).orElse(null);
			if (duplicateByMessageId != null) {
				return duplicateResult(duplicateByMessageId, EmailIngestionDecisionReason.DUPLICATE_MESSAGE_ID);
			}
			String fingerprint = fingerprintFactory.create(command);
			EmailIngestionRecord duplicateByFingerprint = recordRepository.findLatestByHouseholdIdAndFingerprint(source.householdId(), fingerprint)
				.orElse(null);
			if (duplicateByFingerprint != null) {
				return duplicateResult(duplicateByFingerprint, EmailIngestionDecisionReason.DUPLICATE_FINGERPRINT);
			}
			throw exception;
		}
	}

	private EmailIngestionResult processUniqueCandidate(EmailIngestionSource source, ProcessEmailIngestionCommand command) {
		String fingerprint = fingerprintFactory.create(command);
		EmailIngestionRecord duplicateByFingerprint = recordRepository.findLatestByHouseholdIdAndFingerprint(source.householdId(), fingerprint)
			.orElse(null);
		if (duplicateByFingerprint != null) {
			return duplicateResult(duplicateByFingerprint, EmailIngestionDecisionReason.DUPLICATE_FINGERPRINT);
		}
		if (command.classification() == EmailIngestionClassification.IRRELEVANT) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.IGNORED, EmailIngestionDecisionReason.IRRELEVANT_CLASSIFICATION, null);
		}
		if (command.desiredDecision() == EmailIngestionDesiredDecision.IGNORE) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.IGNORED, EmailIngestionDecisionReason.DESIRED_IGNORE, null);
		}
		if (command.desiredDecision() == EmailIngestionDesiredDecision.REVIEW) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.REVIEW_REQUIRED, EmailIngestionDecisionReason.REVIEW_REQUESTED, null);
		}
		if (command.confidence().compareTo(source.reviewMinConfidence()) < 0) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.IGNORED, EmailIngestionDecisionReason.LOW_CONFIDENCE, null);
		}
		if (!SUPPORTED_CURRENCY.equals(command.currency())) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.REVIEW_REQUIRED, EmailIngestionDecisionReason.UNSUPPORTED_CURRENCY, null);
		}
		if (command.totalAmount() == null) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.REVIEW_REQUIRED, EmailIngestionDecisionReason.MISSING_TOTAL_AMOUNT, null);
		}
		if (hasItemTotalMismatch(command)) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.REVIEW_REQUIRED, EmailIngestionDecisionReason.ITEM_TOTAL_MISMATCH, null);
		}
		if (command.confidence().compareTo(source.autoImportMinConfidence()) < 0) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.REVIEW_REQUIRED, EmailIngestionDecisionReason.REVIEW_REQUESTED, null);
		}
		try {
			ExpenseResponse expense = expenseImportService.importExpense(source.householdId(), source.sourceAccount(), command);
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.AUTO_IMPORTED, EmailIngestionDecisionReason.IMPORTED, expense.id());
		} catch (EmailIngestionImportReviewException exception) {
			return persistResult(source, command, fingerprint, EmailIngestionFinalDecision.REVIEW_REQUIRED, exception.getReason(), null);
		}
	}

	private EmailIngestionResult duplicateResult(EmailIngestionRecord existing, EmailIngestionDecisionReason reason) {
		return new EmailIngestionResult(
			existing.id(),
			existing.householdId(),
			EmailIngestionFinalDecision.IGNORED,
			reason,
			existing.importedExpenseId(),
			true,
			messageFor(EmailIngestionFinalDecision.IGNORED, reason)
		);
	}

	private EmailIngestionResult persistResult(
		EmailIngestionSource source,
		ProcessEmailIngestionCommand command,
		String fingerprint,
		EmailIngestionFinalDecision finalDecision,
		EmailIngestionDecisionReason reason,
		Long importedExpenseId
	) {
		EmailIngestionRecord saved = recordRepository.save(new EmailIngestionRecord(
			null,
			source.householdId(),
			source.id(),
			source.sourceAccount(),
			source.normalizedSourceAccount(),
			command.externalMessageId(),
			command.sender(),
			command.subject(),
			command.receivedAt(),
			command.merchantOrPayee(),
			command.suggestedCategoryName(),
			command.suggestedSubcategoryName(),
			command.totalAmount(),
			command.dueDate(),
			command.occurredOn(),
			command.currency(),
			command.summary(),
			command.classification(),
			command.confidence(),
			command.rawReference(),
			command.desiredDecision(),
			finalDecision,
			reason,
			fingerprint,
			importedExpenseId,
			null,
			null,
			command.items()
		));
		return new EmailIngestionResult(
			saved.id(),
			saved.householdId(),
			finalDecision,
			reason,
			importedExpenseId,
			false,
			messageFor(finalDecision, reason)
		);
	}

	private boolean hasItemTotalMismatch(ProcessEmailIngestionCommand command) {
		if (command.items() == null || command.items().isEmpty() || command.totalAmount() == null) {
			return false;
		}
		List<BigDecimal> itemAmounts = command.items().stream()
			.map(EmailIngestionItem::amount)
			.filter(amount -> amount != null)
			.toList();
		if (itemAmounts.isEmpty() || itemAmounts.size() != command.items().size()) {
			return false;
		}
		BigDecimal itemsTotal = itemAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return itemsTotal.subtract(command.totalAmount()).abs().compareTo(new BigDecimal("0.01")) > 0;
	}

	private ProcessEmailIngestionCommand normalizeCommand(ProcessEmailIngestionCommand command, String normalizedSourceAccount) {
		return new ProcessEmailIngestionCommand(
			normalizedSourceAccount,
			normalizeRequired(command.externalMessageId(), "externalMessageId"),
			normalizeRequired(command.sender(), "sender"),
			normalizeRequired(command.subject(), "subject"),
			command.receivedAt() == null ? OffsetDateTime.now() : command.receivedAt(),
			normalizeOptional(command.merchantOrPayee()),
			normalizeOptional(command.suggestedCategoryName()),
			normalizeOptional(command.suggestedSubcategoryName()),
			command.totalAmount(),
			command.dueDate(),
			command.occurredOn(),
			normalizeRequired(command.currency(), "currency").toUpperCase(Locale.ROOT),
			command.items() == null ? List.of() : command.items().stream()
				.map(item -> new EmailIngestionItem(normalizeOptional(item.description()), item.amount(), item.quantity()))
				.toList(),
			normalizeOptional(command.summary()),
			command.classification(),
			command.confidence(),
			normalizeRequired(command.rawReference(), "rawReference"),
			command.desiredDecision()
		);
	}

	private String normalizeRequired(String value, String field) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value.trim();
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private IllegalArgumentException unmappedSourceAccount(String normalizedSourceAccount) {
		auditLogger.sourceAccountRejected(normalizedSourceAccount, "unmapped_source_account");
		return new IllegalArgumentException("Source account is not mapped to an active household");
	}

	private String messageFor(EmailIngestionFinalDecision decision, EmailIngestionDecisionReason reason) {
		return switch (reason) {
			case IMPORTED -> "Candidate was imported into expenses";
			case MANUALLY_IMPORTED -> "Candidate was approved manually and imported into expenses";
			case REVIEW_REQUESTED -> "Candidate requires manual review";
			case MANUALLY_REJECTED -> "Candidate was rejected during manual review";
			case IRRELEVANT_CLASSIFICATION -> "Candidate was ignored because it is irrelevant";
			case DESIRED_IGNORE -> "Candidate was ignored by requested decision";
			case LOW_CONFIDENCE -> "Candidate was ignored because confidence is below review threshold";
			case DUPLICATE_MESSAGE_ID -> "Candidate was ignored because the external message was already processed";
			case DUPLICATE_FINGERPRINT -> "Candidate was ignored because it matches an existing ingestion fingerprint";
			case UNSUPPORTED_CURRENCY -> "Candidate requires review because the currency is not supported for auto import";
			case MISSING_TOTAL_AMOUNT -> "Candidate requires review because total amount is missing";
			case ITEM_TOTAL_MISMATCH -> "Candidate requires review because item totals do not match the total amount";
			case CATEGORY_NOT_FOUND -> "Candidate requires review because the suggested category is not mapped";
			case SUBCATEGORY_NOT_FOUND -> "Candidate requires review because the suggested subcategory is not mapped";
			case SUBCATEGORY_REQUIRED -> "Candidate requires review because a subcategory could not be resolved";
		};
	}
}
