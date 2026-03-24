package com.gilrossi.despesas.identity;

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
@Table(name = "users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 160)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "credentials_updated_at", nullable = false)
	private Instant credentialsUpdatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "platform_role", nullable = false, length = 32)
	private PlatformUserRole platformRole = PlatformUserRole.STANDARD_USER;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected AppUser() {
	}

	public AppUser(String name, String email, String passwordHash) {
		this(name, email, passwordHash, PlatformUserRole.STANDARD_USER);
	}

	public AppUser(String name, String email, String passwordHash, PlatformUserRole platformRole) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.platformRole = platformRole;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getCredentialsUpdatedAt() {
		return credentialsUpdatedAt;
	}

	public void changePasswordHash(String passwordHash, Instant changedAt) {
		this.passwordHash = passwordHash;
		this.credentialsUpdatedAt = changedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public PlatformUserRole getPlatformRole() {
		return platformRole;
	}

	public void setPlatformRole(PlatformUserRole platformRole) {
		this.platformRole = platformRole;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		if (this.credentialsUpdatedAt == null) {
			this.credentialsUpdatedAt = now;
		}
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
