package com.gilrossi.despesas.audit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistedAuditEventRecorderTest {

	@Mock
	private PersistedAuditEventTransactionalWriter transactionalWriter;

	@Test
	void deve_gravar_sem_quebrar_fluxo_quando_writer_funciona() {
		PersistedAuditEventRecorder recorder = new PersistedAuditEventRecorder(transactionalWriter);
		PersistedAuditEventCommand command = PersistedAuditEventCommand.event(
			PersistedAuditEventCategory.ACCESS,
			"dashboard_read",
			PersistedAuditEventStatus.SUCCESS
		).userId(1L).householdId(7L).requestMethod("GET").requestPath("/api/v1/dashboard").build();

		recorder.recordSafely(command);

		verify(transactionalWriter).record(command);
	}

	@Test
	void deve_ignorar_falha_do_writer() {
		PersistedAuditEventRecorder recorder = new PersistedAuditEventRecorder(transactionalWriter);
		PersistedAuditEventCommand command = PersistedAuditEventCommand.event(
			PersistedAuditEventCategory.AUTH,
			"login",
			PersistedAuditEventStatus.FAILURE
		).build();
		doThrow(new RuntimeException("boom")).when(transactionalWriter).record(command);

		assertThatCode(() -> recorder.recordSafely(command)).doesNotThrowAnyException();
		verify(transactionalWriter).record(command);
	}
}
