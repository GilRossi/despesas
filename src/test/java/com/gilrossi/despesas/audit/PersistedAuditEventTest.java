package com.gilrossi.despesas.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PersistedAuditEventTest {

	@Test
	void deve_expor_todos_os_campos_do_evento_persistido() {
		Instant occurredAt = Instant.parse("2026-03-29T18:00:00Z");
		Instant purgeAfter = Instant.parse("2026-04-28T18:00:00Z");
		PersistedAuditEvent event = new PersistedAuditEvent(
			occurredAt,
			purgeAfter,
			PersistedAuditEventCategory.AUTH,
			"login_succeeded",
			PersistedAuditEventStatus.SUCCESS,
			10L,
			20L,
			"OWNER",
			"auth-login",
			"POST",
			"/api/v1/auth/login",
			"user:10",
			"household:20",
			"AUTH_OK",
			"{\"safe\":true}"
		);

		assertThat(event.getId()).isNull();
		assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
		assertThat(event.getPurgeAfter()).isEqualTo(purgeAfter);
		assertThat(event.getCategory()).isEqualTo(PersistedAuditEventCategory.AUTH);
		assertThat(event.getEventType()).isEqualTo("login_succeeded");
		assertThat(event.getStatus()).isEqualTo(PersistedAuditEventStatus.SUCCESS);
		assertThat(event.getUserId()).isEqualTo(10L);
		assertThat(event.getHouseholdId()).isEqualTo(20L);
		assertThat(event.getActorRole()).isEqualTo("OWNER");
		assertThat(event.getSourceKey()).isEqualTo("auth-login");
		assertThat(event.getRequestMethod()).isEqualTo("POST");
		assertThat(event.getRequestPath()).isEqualTo("/api/v1/auth/login");
		assertThat(event.getPrimaryReference()).isEqualTo("user:10");
		assertThat(event.getSecondaryReference()).isEqualTo("household:20");
		assertThat(event.getDetailCode()).isEqualTo("AUTH_OK");
		assertThat(event.getSafeContextJson()).isEqualTo("{\"safe\":true}");
	}
}
