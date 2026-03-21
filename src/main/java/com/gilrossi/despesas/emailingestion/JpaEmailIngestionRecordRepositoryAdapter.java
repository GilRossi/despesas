package com.gilrossi.despesas.emailingestion;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.emailingestion.persistence.EmailIngestionItemJpaEntity;
import com.gilrossi.despesas.emailingestion.persistence.EmailIngestionItemJpaRepository;
import com.gilrossi.despesas.emailingestion.persistence.EmailIngestionRecordJpaEntity;
import com.gilrossi.despesas.emailingestion.persistence.EmailIngestionRecordJpaRepository;

@Repository
public class JpaEmailIngestionRecordRepositoryAdapter implements EmailIngestionRecordRepository {

	private final EmailIngestionRecordJpaRepository recordRepository;
	private final EmailIngestionItemJpaRepository itemRepository;

	public JpaEmailIngestionRecordRepositoryAdapter(
		EmailIngestionRecordJpaRepository recordRepository,
		EmailIngestionItemJpaRepository itemRepository
	) {
		this.recordRepository = recordRepository;
		this.itemRepository = itemRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailIngestionRecord> findBySourceAccountAndExternalMessageId(String normalizedSourceAccount, String externalMessageId) {
		return recordRepository.findByNormalizedSourceAccountAndExternalMessageId(normalizedSourceAccount, externalMessageId)
			.map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailIngestionRecord> findLatestByHouseholdIdAndFingerprint(Long householdId, String fingerprint) {
		return recordRepository.findFirstByHouseholdIdAndFingerprintOrderByCreatedAtDescIdDesc(householdId, fingerprint)
			.map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailIngestionRecord> findByIdAndHouseholdId(Long id, Long householdId) {
		return recordRepository.findByIdAndHouseholdId(id, householdId)
			.map(this::toDomain);
	}

	@Override
	@Transactional
	public Optional<EmailIngestionRecord> findByIdAndHouseholdIdForUpdate(Long id, Long householdId) {
		return recordRepository.findByIdAndHouseholdIdForUpdate(id, householdId)
			.map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmailIngestionRecord> findAllByHouseholdId(Long householdId) {
		return recordRepository.findAllByHouseholdIdOrderByCreatedAtDescIdDesc(householdId).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmailIngestionRecord> findAllPendingReviewByHouseholdId(Long householdId) {
		return recordRepository.findAllByHouseholdIdAndFinalDecisionOrderByCreatedAtDescIdDesc(
				householdId,
				EmailIngestionFinalDecision.REVIEW_REQUIRED
			).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional
	public EmailIngestionRecord save(EmailIngestionRecord record) {
		boolean isNew = record.id() == null;
		EmailIngestionRecordJpaEntity entity = isNew
			? new EmailIngestionRecordJpaEntity()
			: recordRepository.findById(record.id()).orElseThrow(() -> new IllegalArgumentException("Email ingestion record not found"));
		applyRecord(entity, record);
		EmailIngestionRecordJpaEntity saved = recordRepository.saveAndFlush(entity);
		if (isNew) {
			saveItems(saved.getId(), record.items());
		}
		return toDomain(saved);
	}

	private void applyRecord(EmailIngestionRecordJpaEntity entity, EmailIngestionRecord record) {
		entity.setHouseholdId(record.householdId());
		entity.setSourceId(record.sourceId());
		entity.setSourceAccount(record.sourceAccount());
		entity.setNormalizedSourceAccount(record.normalizedSourceAccount());
		entity.setExternalMessageId(record.externalMessageId());
		entity.setSender(record.sender());
		entity.setSubject(record.subject());
		entity.setReceivedAt(record.receivedAt());
		entity.setMerchantOrPayee(record.merchantOrPayee());
		entity.setSuggestedCategoryName(record.suggestedCategoryName());
		entity.setSuggestedSubcategoryName(record.suggestedSubcategoryName());
		entity.setTotalAmount(record.totalAmount());
		entity.setDueDate(record.dueDate());
		entity.setOccurredOn(record.occurredOn());
		entity.setCurrency(record.currency());
		entity.setSummary(record.summary());
		entity.setClassification(record.classification());
		entity.setConfidence(record.confidence());
		entity.setRawReference(record.rawReference());
		entity.setDesiredDecision(record.desiredDecision());
		entity.setFinalDecision(record.finalDecision());
		entity.setDecisionReason(record.decisionReason());
		entity.setFingerprint(record.fingerprint());
		entity.setImportedExpenseId(record.importedExpenseId());
	}

	private void saveItems(Long ingestionId, List<EmailIngestionItem> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		for (int index = 0; index < items.size(); index++) {
			EmailIngestionItem item = items.get(index);
			EmailIngestionItemJpaEntity entity = new EmailIngestionItemJpaEntity();
			entity.setIngestionId(ingestionId);
			entity.setLineNumber(index + 1);
			entity.setDescription(item.description());
			entity.setAmount(item.amount());
			entity.setQuantity(item.quantity());
			itemRepository.save(entity);
		}
		itemRepository.flush();
	}

	private EmailIngestionRecord toDomain(EmailIngestionRecordJpaEntity entity) {
		List<EmailIngestionItem> items = itemRepository.findAllByIngestionIdOrderByLineNumberAsc(entity.getId()).stream()
			.map(item -> new EmailIngestionItem(item.getDescription(), item.getAmount(), item.getQuantity()))
			.toList();
		return new EmailIngestionRecord(
			entity.getId(),
			entity.getHouseholdId(),
			entity.getSourceId(),
			entity.getSourceAccount(),
			entity.getNormalizedSourceAccount(),
			entity.getExternalMessageId(),
			entity.getSender(),
			entity.getSubject(),
			entity.getReceivedAt(),
			entity.getMerchantOrPayee(),
			entity.getSuggestedCategoryName(),
			entity.getSuggestedSubcategoryName(),
			entity.getTotalAmount(),
			entity.getDueDate(),
			entity.getOccurredOn(),
			entity.getCurrency(),
			entity.getSummary(),
			entity.getClassification(),
			entity.getConfidence(),
			entity.getRawReference(),
			entity.getDesiredDecision(),
			entity.getFinalDecision(),
			entity.getDecisionReason(),
			entity.getFingerprint(),
			entity.getImportedExpenseId(),
			entity.getCreatedAt(),
			entity.getUpdatedAt(),
			items
		);
	}
}
