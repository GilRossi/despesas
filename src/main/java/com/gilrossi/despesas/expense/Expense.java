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

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

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
		LocalDate dueDate,
		ExpenseContext context,
		Long categoryId,
		String categoryNameSnapshot,
		Long subcategoryId,
		String subcategoryNameSnapshot,
		String notes
	) {
		this.householdId = householdId;
		this.description = description;
		this.amount = amount;
		this.dueDate = dueDate;
		this.context = context;
		this.categoryId = categoryId;
		this.categoryNameSnapshot = categoryNameSnapshot;
		this.subcategoryId = subcategoryId;
		this.subcategoryNameSnapshot = subcategoryNameSnapshot;
		this.notes = notes;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
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
			context,
			categoryId,
			categoryNameSnapshot,
			subcategoryId,
			subcategoryNameSnapshot,
			notes
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
