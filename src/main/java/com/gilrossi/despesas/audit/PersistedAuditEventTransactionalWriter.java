package com.gilrossi.despesas.audit;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
class PersistedAuditEventTransactionalWriter {

	private final PersistedAuditEventRepository repository;
	private final AuditTrailProperties properties;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	PersistedAuditEventTransactionalWriter(
		PersistedAuditEventRepository repository,
		AuditTrailProperties properties,
		ObjectMapper objectMapper
	) {
		this(repository, properties, objectMapper, Clock.systemUTC());
	}

	PersistedAuditEventTransactionalWriter(
		PersistedAuditEventRepository repository,
		AuditTrailProperties properties,
		ObjectMapper objectMapper,
		Clock clock
	) {
		this.repository = repository;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void record(PersistedAuditEventCommand command) {
		Instant occurredAt = clock.instant();
		repository.save(new PersistedAuditEvent(
			occurredAt,
			occurredAt.plusSeconds((long) properties.getRetentionDays() * 86_400L),
			command.category(),
			command.eventType(),
			command.status(),
			command.userId(),
			command.householdId(),
			command.actorRole(),
			command.sourceKey(),
			command.requestMethod(),
			command.requestPath(),
			command.primaryReference(),
			command.secondaryReference(),
			command.detailCode(),
			safeContextJson(command)
		));
	}

	private String safeContextJson(PersistedAuditEventCommand command) {
		if (command.safeContext().isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(command.safeContext());
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Unable to serialize audit safe context", exception);
		}
	}
}
