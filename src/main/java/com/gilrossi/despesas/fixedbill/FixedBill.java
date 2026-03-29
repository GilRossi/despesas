package com.gilrossi.despesas.fixedbill;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gilrossi.despesas.expense.ExpenseContext;

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
@Table(name = "fixed_bills")
public class FixedBill {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "household_id", nullable = false)
	private Long householdId;

	@Column(nullable = false, length = 140)
	private String description;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "first_due_date", nullable = false)
	private LocalDate firstDueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FixedBillFrequency frequency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ExpenseContext context;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "category_name_snapshot", nullable = false, length = 80)
	private String categoryNameSnapshot;

	@Column(name = "subcategory_id", nullable = false)
	private Long subcategoryId;

	@Column(name = "subcategory_name_snapshot", nullable = false, length = 80)
	private String subcategoryNameSnapshot;

	@Column(name = "space_reference_id")
	private Long spaceReferenceId;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected FixedBill() {
	}

	public FixedBill(
		Long householdId,
		String description,
		BigDecimal amount,
		LocalDate firstDueDate,
		FixedBillFrequency frequency,
		ExpenseContext context,
		Long categoryId,
		String categoryNameSnapshot,
		Long subcategoryId,
		String subcategoryNameSnapshot,
		Long spaceReferenceId
	) {
		this.householdId = householdId;
		this.description = description;
		this.amount = amount;
		this.firstDueDate = firstDueDate;
		this.frequency = frequency;
		this.context = context;
		this.categoryId = categoryId;
		this.categoryNameSnapshot = categoryNameSnapshot;
		this.subcategoryId = subcategoryId;
		this.subcategoryNameSnapshot = subcategoryNameSnapshot;
		this.spaceReferenceId = spaceReferenceId;
		this.active = true;
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

	public LocalDate getFirstDueDate() {
		return firstDueDate;
	}

	public void setFirstDueDate(LocalDate firstDueDate) {
		this.firstDueDate = firstDueDate;
	}

	public FixedBillFrequency getFrequency() {
		return frequency;
	}

	public void setFrequency(FixedBillFrequency frequency) {
		this.frequency = frequency;
	}

	public ExpenseContext getContext() {
		return context;
	}

	public void setContext(ExpenseContext context) {
		this.context = context;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryNameSnapshot() {
		return categoryNameSnapshot;
	}

	public void setCategoryNameSnapshot(String categoryNameSnapshot) {
		this.categoryNameSnapshot = categoryNameSnapshot;
	}

	public Long getSubcategoryId() {
		return subcategoryId;
	}

	public void setSubcategoryId(Long subcategoryId) {
		this.subcategoryId = subcategoryId;
	}

	public String getSubcategoryNameSnapshot() {
		return subcategoryNameSnapshot;
	}

	public void setSubcategoryNameSnapshot(String subcategoryNameSnapshot) {
		this.subcategoryNameSnapshot = subcategoryNameSnapshot;
	}

	public Long getSpaceReferenceId() {
		return spaceReferenceId;
	}

	public void setSpaceReferenceId(Long spaceReferenceId) {
		this.spaceReferenceId = spaceReferenceId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
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
