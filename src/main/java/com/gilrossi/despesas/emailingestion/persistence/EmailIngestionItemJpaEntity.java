package com.gilrossi.despesas.emailingestion.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_ingestion_items")
public class EmailIngestionItemJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ingestion_id", nullable = false)
	private Long ingestionId;

	@Column(name = "line_number", nullable = false)
	private Integer lineNumber;

	@Column(name = "description", nullable = false, length = 255)
	private String description;

	@Column(name = "amount", precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "quantity", precision = 12, scale = 3)
	private BigDecimal quantity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getIngestionId() {
		return ingestionId;
	}

	public void setIngestionId(Long ingestionId) {
		this.ingestionId = ingestionId;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
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

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
}
