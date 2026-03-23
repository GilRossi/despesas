package com.gilrossi.despesas.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
	name = "persisted_audit_events",
	indexes = {
		@Index(name = "idx_persisted_audit_events_occurred_at", columnList = "occurred_at"),
		@Index(name = "idx_persisted_audit_events_purge_after", columnList = "purge_after"),
		@Index(name = "idx_persisted_audit_events_category_type", columnList = "category,event_type"),
		@Index(name = "idx_persisted_audit_events_user_occurred", columnList = "user_id,occurred_at"),
		@Index(name = "idx_persisted_audit_events_household_occurred", columnList = "household_id,occurred_at")
	}
)
public class PersistedAuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "purge_after", nullable = false)
	private Instant purgeAfter;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 32)
	private PersistedAuditEventCategory category;

	@Column(name = "event_type", nullable = false, length = 80)
	private String eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	private PersistedAuditEventStatus status;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "household_id")
	private Long householdId;

	@Column(name = "actor_role", length = 40)
	private String actorRole;

	@Column(name = "source_key", length = 120)
	private String sourceKey;

	@Column(name = "request_method", length = 8)
	private String requestMethod;

	@Column(name = "request_path", length = 255)
	private String requestPath;

	@Column(name = "primary_reference", length = 120)
	private String primaryReference;

	@Column(name = "secondary_reference", length = 120)
	private String secondaryReference;

	@Column(name = "detail_code", length = 80)
	private String detailCode;

	@Column(name = "safe_context_json", columnDefinition = "text")
	private String safeContextJson;

	protected PersistedAuditEvent() {
	}

	public PersistedAuditEvent(
		Instant occurredAt,
		Instant purgeAfter,
		PersistedAuditEventCategory category,
		String eventType,
		PersistedAuditEventStatus status,
		Long userId,
		Long householdId,
		String actorRole,
		String sourceKey,
		String requestMethod,
		String requestPath,
		String primaryReference,
		String secondaryReference,
		String detailCode,
		String safeContextJson
	) {
		this.occurredAt = occurredAt;
		this.purgeAfter = purgeAfter;
		this.category = category;
		this.eventType = eventType;
		this.status = status;
		this.userId = userId;
		this.householdId = householdId;
		this.actorRole = actorRole;
		this.sourceKey = sourceKey;
		this.requestMethod = requestMethod;
		this.requestPath = requestPath;
		this.primaryReference = primaryReference;
		this.secondaryReference = secondaryReference;
		this.detailCode = detailCode;
		this.safeContextJson = safeContextJson;
	}

	public Long getId() {
		return id;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Instant getPurgeAfter() {
		return purgeAfter;
	}

	public PersistedAuditEventCategory getCategory() {
		return category;
	}

	public String getEventType() {
		return eventType;
	}

	public PersistedAuditEventStatus getStatus() {
		return status;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getHouseholdId() {
		return householdId;
	}

	public String getActorRole() {
		return actorRole;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public String getPrimaryReference() {
		return primaryReference;
	}

	public String getSecondaryReference() {
		return secondaryReference;
	}

	public String getDetailCode() {
		return detailCode;
	}

	public String getSafeContextJson() {
		return safeContextJson;
	}
}
