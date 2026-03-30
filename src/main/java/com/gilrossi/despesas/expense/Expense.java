package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.Transient;

@Entity
@Table(name = "expenses")
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 140)
	private String description;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "household_id", nullable = false)
	private Long householdId;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "occurred_on", nullable = false)
	private LocalDate occurredOn;

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

	@Column(length = 255)
	private String notes;

	@Column(name = "space_reference_id")
	private Long spaceReferenceId;

	@Transient
	private ExpenseStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Expense() {
	}

	public Expense(
		Long householdId,
		String description,
		BigDecimal amount,
		LocalDate occurredOn,
		LocalDate dueDate,
		ExpenseContext context,
		Long categoryId,
		String categoryNameSnapshot,
		Long subcategoryId,
		String subcategoryNameSnapshot,
		String notes,
		Long spaceReferenceId
	) {
		this.householdId = householdId;
		this.description = description;
		this.amount = amount;
		this.occurredOn = occurredOn;
		this.dueDate = dueDate;
		this.context = context;
		this.categoryId = categoryId;
		this.categoryNameSnapshot = categoryNameSnapshot;
		this.subcategoryId = subcategoryId;
		this.subcategoryNameSnapshot = subcategoryNameSnapshot;
		this.notes = notes;
		this.spaceReferenceId = spaceReferenceId;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	public Expense(
		String description,
		BigDecimal amount,
		LocalDate occurredOn,
		LocalDate dueDate,
		ExpenseContext context,
		Long categoryId,
		String categoryNameSnapshot,
		Long subcategoryId,
		String subcategoryNameSnapshot,
		String notes,
		Long spaceReferenceId
	) {
		this(
			null,
			description,
			amount,
			occurredOn,
			dueDate,
			context,
			categoryId,
			categoryNameSnapshot,
			subcategoryId,
			subcategoryNameSnapshot,
			notes,
			spaceReferenceId
		);
	}

	public Expense(
		Long householdId,
		String description,
		BigDecimal amount,
		LocalDate dueDate,
		ExpenseContext context,
		Long categoryId,
		String categoryNameSnapshot,
		Long subcategoryId,
		String subcategoryNameSnapshot,
		String notes
	) {
		this(
			householdId,
			description,
			amount,
			dueDate,
			dueDate,
			context,
			categoryId,
			categoryNameSnapshot,
			subcategoryId,
			subcategoryNameSnapshot,
			notes,
			null
		);
	}

	public Expense(
		String description,
		BigDecimal amount,
		LocalDate dueDate,
		ExpenseContext context,
		Long categoryId,
		String categoryNameSnapshot,
		Long subcategoryId,
		String subcategoryNameSnapshot,
		String notes
	) {
		this(
			null,
			description,
			amount,
			dueDate,
			dueDate,
			context,
			categoryId,
			categoryNameSnapshot,
			subcategoryId,
			subcategoryNameSnapshot,
			notes,
			null
		);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Long getHouseholdId() {
		return householdId;
	}

	public void setHouseholdId(Long householdId) {
		this.householdId = householdId;
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

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Long getSpaceReferenceId() {
		return spaceReferenceId;
	}

	public void setSpaceReferenceId(Long spaceReferenceId) {
		this.spaceReferenceId = spaceReferenceId;
	}

	public ExpenseStatus getStatus() {
		return status;
	}

	public void setStatus(ExpenseStatus status) {
		this.status = status;
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

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void touch() {
		this.updatedAt = Instant.now();
	}

	public void markDeleted() {
		Instant now = Instant.now();
		this.deletedAt = now;
		this.updatedAt = now;
	}

	public LocalDate getEffectiveDate() {
		return dueDate != null ? dueDate : occurredOn;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
