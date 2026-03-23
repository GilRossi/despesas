package com.gilrossi.despesas.audit;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PersistedAuditEventCommand {

	private final PersistedAuditEventCategory category;
	private final String eventType;
	private final PersistedAuditEventStatus status;
	private final Long userId;
	private final Long householdId;
	private final String actorRole;
	private final String sourceKey;
	private final String requestMethod;
	private final String requestPath;
	private final String primaryReference;
	private final String secondaryReference;
	private final String detailCode;
	private final Map<String, String> safeContext;

	private PersistedAuditEventCommand(Builder builder) {
		this.category = builder.category;
		this.eventType = builder.eventType;
		this.status = builder.status;
		this.userId = builder.userId;
		this.householdId = builder.householdId;
		this.actorRole = builder.actorRole;
		this.sourceKey = builder.sourceKey;
		this.requestMethod = builder.requestMethod;
		this.requestPath = builder.requestPath;
		this.primaryReference = builder.primaryReference;
		this.secondaryReference = builder.secondaryReference;
		this.detailCode = builder.detailCode;
		this.safeContext = Map.copyOf(builder.safeContext);
	}

	public static Builder event(PersistedAuditEventCategory category, String eventType, PersistedAuditEventStatus status) {
		return new Builder(category, eventType, status);
	}

	public PersistedAuditEventCategory category() {
		return category;
	}

	public String eventType() {
		return eventType;
	}

	public PersistedAuditEventStatus status() {
		return status;
	}

	public Long userId() {
		return userId;
	}

	public Long householdId() {
		return householdId;
	}

	public String actorRole() {
		return actorRole;
	}

	public String sourceKey() {
		return sourceKey;
	}

	public String requestMethod() {
		return requestMethod;
	}

	public String requestPath() {
		return requestPath;
	}

	public String primaryReference() {
		return primaryReference;
	}

	public String secondaryReference() {
		return secondaryReference;
	}

	public String detailCode() {
		return detailCode;
	}

	public Map<String, String> safeContext() {
		return safeContext;
	}

	public static final class Builder {

		private final PersistedAuditEventCategory category;
		private final String eventType;
		private final PersistedAuditEventStatus status;
		private Long userId;
		private Long householdId;
		private String actorRole;
		private String sourceKey;
		private String requestMethod;
		private String requestPath;
		private String primaryReference;
		private String secondaryReference;
		private String detailCode;
		private final Map<String, String> safeContext = new LinkedHashMap<>();

		private Builder(PersistedAuditEventCategory category, String eventType, PersistedAuditEventStatus status) {
			this.category = category;
			this.eventType = eventType;
			this.status = status;
		}

		public Builder userId(Long userId) {
			this.userId = userId;
			return this;
		}

		public Builder householdId(Long householdId) {
			this.householdId = householdId;
			return this;
		}

		public Builder actorRole(String actorRole) {
			this.actorRole = actorRole;
			return this;
		}

		public Builder sourceKey(String sourceKey) {
			this.sourceKey = sourceKey;
			return this;
		}

		public Builder requestMethod(String requestMethod) {
			this.requestMethod = requestMethod;
			return this;
		}

		public Builder requestPath(String requestPath) {
			this.requestPath = requestPath;
			return this;
		}

		public Builder primaryReference(String primaryReference) {
			this.primaryReference = primaryReference;
			return this;
		}

		public Builder secondaryReference(String secondaryReference) {
			this.secondaryReference = secondaryReference;
			return this;
		}

		public Builder detailCode(String detailCode) {
			this.detailCode = detailCode;
			return this;
		}

		public Builder safeContext(String key, Object value) {
			if (key != null && !key.isBlank() && value != null) {
				this.safeContext.put(key, value.toString());
			}
			return this;
		}

		public PersistedAuditEventCommand build() {
			return new PersistedAuditEventCommand(this);
		}
	}
}
