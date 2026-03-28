package com.gilrossi.despesas.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "incomes")
public class Income {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "household_id", nullable = false)
	private Long householdId;

	@Column(nullable = false, length = 140)
	private String description;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "received_on", nullable = false)
	private LocalDate receivedOn;

	@Column(name = "space_reference_id")
	private Long spaceReferenceId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Income() {
	}

	public Income(
		Long householdId,
		String description,
		BigDecimal amount,
		LocalDate receivedOn,
		Long spaceReferenceId
	) {
		this.householdId = householdId;
		this.description = description;
		this.amount = amount;
		this.receivedOn = receivedOn;
		this.spaceReferenceId = spaceReferenceId;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getReceivedOn() {
		return receivedOn;
	}

	public void setReceivedOn(LocalDate receivedOn) {
		this.receivedOn = receivedOn;
	}

	public Long getSpaceReferenceId() {
		return spaceReferenceId;
	}

	public void setSpaceReferenceId(Long spaceReferenceId) {
		this.spaceReferenceId = spaceReferenceId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
