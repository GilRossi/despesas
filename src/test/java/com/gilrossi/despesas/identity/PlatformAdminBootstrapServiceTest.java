package com.gilrossi.despesas.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PlatformAdminBootstrapServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private PlatformAdminBootstrapService service;

	@BeforeEach
	void setUp() {
		service = new PlatformAdminBootstrapService(
			appUserRepository,
			passwordEncoder,
			new PlatformAdminBootstrapProperties("Platform Admin", "admin@local.invalid", "senha123")
		);
	}

	@Test
	void deve_criar_primeiro_platform_admin_quando_nao_existir() {
		when(appUserRepository.existsByPlatformRoleAndDeletedAtIsNull(PlatformUserRole.PLATFORM_ADMIN)).thenReturn(false);
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@local.invalid")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("senha123")).thenReturn("{bcrypt}hash");
		when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.bootstrapIfNeeded();

		verify(appUserRepository).save(any(AppUser.class));
	}

	@Test
	void deve_nao_duplicar_platform_admin_quando_ja_existir() {
		when(appUserRepository.existsByPlatformRoleAndDeletedAtIsNull(PlatformUserRole.PLATFORM_ADMIN)).thenReturn(true);

		service.bootstrapIfNeeded();

		verify(appUserRepository, never()).save(any(AppUser.class));
	}

	@Test
	void deve_ignorar_bootstrap_quando_credenciais_estiverem_incompletas() {
		service = new PlatformAdminBootstrapService(
			appUserRepository,
			passwordEncoder,
			new PlatformAdminBootstrapProperties("Platform Admin", "", "")
		);
		when(appUserRepository.existsByPlatformRoleAndDeletedAtIsNull(PlatformUserRole.PLATFORM_ADMIN)).thenReturn(false);

		service.bootstrapIfNeeded();

		verify(appUserRepository, never()).save(any(AppUser.class));
	}

	@Test
	void deve_falhar_quando_email_de_bootstrap_ja_pertencer_a_usuario_nao_admin() {
		AppUser existing = new AppUser("Ana", "admin@local.invalid", "{bcrypt}hash");
		existing.setPlatformRole(PlatformUserRole.STANDARD_USER);
		when(appUserRepository.existsByPlatformRoleAndDeletedAtIsNull(PlatformUserRole.PLATFORM_ADMIN)).thenReturn(false);
		when(appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@local.invalid")).thenReturn(Optional.of(existing));

		IllegalStateException exception = assertThrows(IllegalStateException.class, service::bootstrapIfNeeded);

		assertEquals("Bootstrap admin email already belongs to a non-admin user", exception.getMessage());
	}
}
