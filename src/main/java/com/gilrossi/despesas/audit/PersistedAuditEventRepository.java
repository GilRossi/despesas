package com.gilrossi.despesas.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersistedAuditEventRepository extends JpaRepository<PersistedAuditEvent, Long> {

	List<PersistedAuditEvent> findAllByEventTypeOrderByIdAsc(String eventType);
}
