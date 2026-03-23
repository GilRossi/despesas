package com.gilrossi.despesas.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PersistedAuditEventRecorder {

	private static final Logger LOGGER = LoggerFactory.getLogger(PersistedAuditEventRecorder.class);

	private final PersistedAuditEventTransactionalWriter transactionalWriter;

	public PersistedAuditEventRecorder(PersistedAuditEventTransactionalWriter transactionalWriter) {
		this.transactionalWriter = transactionalWriter;
	}

	public void recordSafely(PersistedAuditEventCommand command) {
		try {
			transactionalWriter.record(command);
		} catch (RuntimeException exception) {
			LOGGER.warn(
				"event=audit_trail_persist_failure category={} eventType={} exceptionClass={}",
				command.category(),
				command.eventType(),
				exception.getClass().getSimpleName()
			);
		}
	}
}
