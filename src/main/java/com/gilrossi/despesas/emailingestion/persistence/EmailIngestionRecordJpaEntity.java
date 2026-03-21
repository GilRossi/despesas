package com.gilrossi.despesas.emailingestion.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.gilrossi.despesas.emailingestion.EmailIngestionClassification;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionDesiredDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_ingestions")
public class EmailIngestionRecordJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "household_id", nullable = false)
	private Long householdId;

	@Column(name = "source_id", nullable = false)
	private Long sourceId;

	@Column(name = "source_account", nullable = false, length = 160)
	private String sourceAccount;

	@Column(name = "normalized_source_account", nullable = false, length = 160)
	private String normalizedSourceAccount;

	@Column(name = "external_message_id", nullable = false, length = 255)
	private String externalMessageId;

	@Column(name = "sender", nullable = false, length = 255)
	private String sender;

	@Column(name = "subject", nullable = false, length = 255)
	private String subject;

	@Column(name = "received_at", nullable = false)
	private OffsetDateTime receivedAt;

	@Column(name = "merchant_or_payee", length = 140)
	private String merchantOrPayee;

	@Column(name = "suggested_category_name", length = 80)
	private String suggestedCategoryName;

	@Column(name = "suggested_subcategory_name", length = 80)
	private String suggestedSubcategoryName;

	@Column(name = "total_amount", precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "occurred_on")
	private LocalDate occurredOn;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "summary", length = 500)
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(name = "classification", nullable = false, length = 40)
	private EmailIngestionClassification classification;

	@Column(name = "confidence", nullable = false, precision = 4, scale = 3)
	private BigDecimal confidence;

	@Column(name = "raw_reference", nullable = false, length = 500)
	private String rawReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "desired_decision", nullable = false, length = 20)
	private EmailIngestionDesiredDecision desiredDecision;

	@Enumerated(EnumType.STRING)
	@Column(name = "final_decision", nullable = false, length = 20)
	private EmailIngestionFinalDecision finalDecision;

	@Enumerated(EnumType.STRING)
	@Column(name = "decision_reason", nullable = false, length = 40)
	private EmailIngestionDecisionReason decisionReason;

	@Column(name = "fingerprint", nullable = false, length = 64)
	private String fingerprint;

	@Column(name = "imported_expense_id")
	private Long importedExpenseId;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	void prePersist() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getHouseholdId() {
		return householdId;
	}

	public void setHouseholdId(Long householdId) {
		this.householdId = householdId;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourceAccount() {
		return sourceAccount;
	}

	public void setSourceAccount(String sourceAccount) {
		this.sourceAccount = sourceAccount;
	}

	public String getNormalizedSourceAccount() {
		return normalizedSourceAccount;
	}

	public void setNormalizedSourceAccount(String normalizedSourceAccount) {
		this.normalizedSourceAccount = normalizedSourceAccount;
	}

	public String getExternalMessageId() {
		return externalMessageId;
	}

	public void setExternalMessageId(String externalMessageId) {
		this.externalMessageId = externalMessageId;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public OffsetDateTime getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(OffsetDateTime receivedAt) {
		this.receivedAt = receivedAt;
	}

	public String getMerchantOrPayee() {
		return merchantOrPayee;
	}

	public void setMerchantOrPayee(String merchantOrPayee) {
		this.merchantOrPayee = merchantOrPayee;
	}

	public String getSuggestedCategoryName() {
		return suggestedCategoryName;
	}

	public void setSuggestedCategoryName(String suggestedCategoryName) {
		this.suggestedCategoryName = suggestedCategoryName;
	}

	public String getSuggestedSubcategoryName() {
		return suggestedSubcategoryName;
	}

	public void setSuggestedSubcategoryName(String suggestedSubcategoryName) {
		this.suggestedSubcategoryName = suggestedSubcategoryName;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDate getOccurredOn() {
		return occurredOn;
	}

	public void setOccurredOn(LocalDate occurredOn) {
		this.occurredOn = occurredOn;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public EmailIngestionClassification getClassification() {
		return classification;
	}

	public void setClassification(EmailIngestionClassification classification) {
		this.classification = classification;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public void setConfidence(BigDecimal confidence) {
		this.confidence = confidence;
	}

	public String getRawReference() {
		return rawReference;
	}

	public void setRawReference(String rawReference) {
		this.rawReference = rawReference;
	}

	public EmailIngestionDesiredDecision getDesiredDecision() {
		return desiredDecision;
	}

	public void setDesiredDecision(EmailIngestionDesiredDecision desiredDecision) {
		this.desiredDecision = desiredDecision;
	}

	public EmailIngestionFinalDecision getFinalDecision() {
		return finalDecision;
	}

	public void setFinalDecision(EmailIngestionFinalDecision finalDecision) {
		this.finalDecision = finalDecision;
	}

	public EmailIngestionDecisionReason getDecisionReason() {
		return decisionReason;
	}

	public void setDecisionReason(EmailIngestionDecisionReason decisionReason) {
		this.decisionReason = decisionReason;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	public Long getImportedExpenseId() {
		return importedExpenseId;
	}

	public void setImportedExpenseId(Long importedExpenseId) {
		this.importedExpenseId = importedExpenseId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
