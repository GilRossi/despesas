package com.gilrossi.despesas.security;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "operational_request_nonces",
	uniqueConstraints = @UniqueConstraint(
		name = "uq_operational_request_nonces_key_nonce",
		columnNames = {"key_id", "nonce_hash"}
	),
	indexes = @Index(name = "idx_operational_request_nonces_expires_at", columnList = "expires_at")
)
public class OperationalRequestNonceRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "key_id", nullable = false, length = 80)
	private String keyId;

	@Column(name = "nonce_hash", nullable = false, length = 64)
	private String nonceHash;

	@Column(name = "request_method", nullable = false, length = 8)
	private String requestMethod;

	@Column(name = "request_path", nullable = false, length = 255)
	private String requestPath;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected OperationalRequestNonceRecord() {
	}

	public OperationalRequestNonceRecord(
		String keyId,
		String nonceHash,
		String requestMethod,
		String requestPath,
		Instant expiresAt
	) {
		this.keyId = keyId;
		this.nonceHash = nonceHash;
		this.requestMethod = requestMethod;
		this.requestPath = requestPath;
		this.expiresAt = expiresAt;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
