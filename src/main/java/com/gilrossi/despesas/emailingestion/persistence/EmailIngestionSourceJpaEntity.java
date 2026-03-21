package com.gilrossi.despesas.emailingestion.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_ingestion_sources")
public class EmailIngestionSourceJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "household_id", nullable = false)
	private Long householdId;

	@Column(name = "source_account", nullable = false, length = 160)
	private String sourceAccount;

	@Column(name = "normalized_source_account", nullable = false, length = 160, unique = true)
	private String normalizedSourceAccount;

	@Column(name = "label", length = 120)
	private String label;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "auto_import_min_confidence", nullable = false, precision = 4, scale = 3)
	private BigDecimal autoImportMinConfidence;

	@Column(name = "review_min_confidence", nullable = false, precision = 4, scale = 3)
	private BigDecimal reviewMinConfidence;

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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public BigDecimal getAutoImportMinConfidence() {
		return autoImportMinConfidence;
	}

	public void setAutoImportMinConfidence(BigDecimal autoImportMinConfidence) {
		this.autoImportMinConfidence = autoImportMinConfidence;
	}

	public BigDecimal getReviewMinConfidence() {
		return reviewMinConfidence;
	}

	public void setReviewMinConfidence(BigDecimal reviewMinConfidence) {
		this.reviewMinConfidence = reviewMinConfidence;
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
