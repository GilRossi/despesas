package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class EmailIngestionSourceService {

	private static final BigDecimal DEFAULT_AUTO_IMPORT_MIN_CONFIDENCE = new BigDecimal("0.90");
	private static final BigDecimal DEFAULT_REVIEW_MIN_CONFIDENCE = new BigDecimal("0.65");

	private final EmailIngestionSourceRepository repository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public EmailIngestionSourceService(
		EmailIngestionSourceRepository repository,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.repository = repository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public List<EmailIngestionSource> list() {
		return repository.findAllByHouseholdId(currentHouseholdProvider.requireHouseholdId());
	}

	@Transactional
	public EmailIngestionSource register(RegisterEmailIngestionSourceCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		String sourceAccount = normalizeRequired(command.sourceAccount(), "sourceAccount");
		String normalizedSourceAccount = sourceAccount.toLowerCase(Locale.ROOT);
		BigDecimal autoImportMinConfidence = command.autoImportMinConfidence() == null
			? DEFAULT_AUTO_IMPORT_MIN_CONFIDENCE
			: normalizeConfidence(command.autoImportMinConfidence(), "autoImportMinConfidence");
		BigDecimal reviewMinConfidence = command.reviewMinConfidence() == null
			? DEFAULT_REVIEW_MIN_CONFIDENCE
			: normalizeConfidence(command.reviewMinConfidence(), "reviewMinConfidence");
		if (autoImportMinConfidence.compareTo(reviewMinConfidence) < 0) {
			throw new IllegalArgumentException("autoImportMinConfidence must be greater than or equal to reviewMinConfidence");
		}
		repository.findByNormalizedSourceAccount(normalizedSourceAccount)
			.ifPresent(existing -> {
				throw new DuplicateEmailIngestionSourceException(sourceAccount);
			});
		return repository.save(new EmailIngestionSource(
			null,
			householdId,
			sourceAccount,
			normalizedSourceAccount,
			normalizeOptional(command.label()),
			true,
			autoImportMinConfidence,
			reviewMinConfidence,
			null,
			null
		));
	}

	private BigDecimal normalizeConfidence(BigDecimal value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " must not be null");
		}
		if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException(field + " must be between 0 and 1");
		}
		return value;
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
}
