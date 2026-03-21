package com.gilrossi.despesas.emailingestion.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailIngestionItemJpaRepository extends JpaRepository<EmailIngestionItemJpaEntity, Long> {

	List<EmailIngestionItemJpaEntity> findAllByIngestionIdOrderByLineNumberAsc(Long ingestionId);

	void deleteAllByIngestionId(Long ingestionId);
}
