package com.gilrossi.despesas.identity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

	boolean existsByPlatformRoleAndDeletedAtIsNull(PlatformUserRole platformRole);
}
