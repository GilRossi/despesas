package com.gilrossi.despesas.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalReplayProtectionService {

	private final OperationalRequestNonceRecordRepository repository;

	public OperationalReplayProtectionService(OperationalRequestNonceRecordRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void registerNonce(
		String keyId,
		String nonce,
		String requestMethod,
		String requestPath,
		Instant now,
		long nonceTtlSeconds
	) {
		repository.deleteExpiredBefore(now);
		try {
			repository.saveAndFlush(new OperationalRequestNonceRecord(
				keyId,
				OperationalRequestSignatureSupport.sha256Hex(nonce.getBytes(StandardCharsets.UTF_8)),
				requestMethod,
				requestPath,
				now.plusSeconds(nonceTtlSeconds)
			));
		} catch (DataIntegrityViolationException exception) {
			throw new OperationalRequestValidationException(
				OperationalRequestRejectionReason.REPLAY_DETECTED,
				keyId,
				OperationalRequestSignatureSupport.fingerprint(nonce),
				"-",
				null
			);
		}
	}
}
