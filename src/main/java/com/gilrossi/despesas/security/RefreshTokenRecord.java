package com.gilrossi.despesas.security;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_refresh_tokens")
public class RefreshTokenRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_id", nullable = false, length = 36, unique = true)
	private String tokenId;

	@Column(name = "family_id", nullable = false, length = 36)
	private String familyId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, length = 255)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_id", length = 36)
	private String replacedByTokenId;

	@Enumerated(EnumType.STRING)
	@Column(name = "revocation_reason", length = 32)
	private RefreshTokenRevocationReason revocationReason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RefreshTokenRecord() {
	}

	public RefreshTokenRecord(String tokenId, String familyId, Long userId, String tokenHash, Instant expiresAt) {
		this.tokenId = tokenId;
		this.familyId = familyId;
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public Long getId() {
		return id;
	}

	public String getTokenId() {
		return tokenId;
	}

	public String getFamilyId() {
		return familyId;
	}

	public Long getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public String getReplacedByTokenId() {
		return replacedByTokenId;
	}

	public RefreshTokenRevocationReason getRevocationReason() {
		return revocationReason;
	}

	public boolean isExpired(Instant now) {
		return !expiresAt.isAfter(now);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean wasRotated() {
		return replacedByTokenId != null;
	}

	public void markRotated(String replacementTokenId, Instant now) {
		this.lastUsedAt = now;
		this.revokedAt = now;
		this.replacedByTokenId = replacementTokenId;
		this.revocationReason = RefreshTokenRevocationReason.ROTATED;
	}

	public void revoke(RefreshTokenRevocationReason reason, Instant now) {
		if (this.revokedAt == null) {
			this.revokedAt = now;
		}
		this.lastUsedAt = now;
		if (this.revocationReason == null || reason != RefreshTokenRevocationReason.ROTATED) {
			this.revocationReason = reason;
		}
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
