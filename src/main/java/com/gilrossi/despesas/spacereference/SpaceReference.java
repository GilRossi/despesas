package com.gilrossi.despesas.spacereference;

import java.time.OffsetDateTime;

public class SpaceReference {

	private Long id;
	private Long householdId;
	private SpaceReferenceType type;
	private String name;
	private String normalizedName;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private OffsetDateTime deletedAt;

	public SpaceReference() {
	}

	public SpaceReference(
		Long id,
		Long householdId,
		SpaceReferenceType type,
		String name,
		String normalizedName,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		OffsetDateTime deletedAt
	) {
		this.id = id;
		this.householdId = householdId;
		this.type = type;
		this.name = name;
		this.normalizedName = normalizedName;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.deletedAt = deletedAt;
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

	public SpaceReferenceType getType() {
		return type;
	}

	public void setType(SpaceReferenceType type) {
		this.type = type;
	}

	public SpaceReferenceTypeGroup getTypeGroup() {
		return type == null ? null : type.group();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public void setNormalizedName(String normalizedName) {
		this.normalizedName = normalizedName;
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

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(OffsetDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}
