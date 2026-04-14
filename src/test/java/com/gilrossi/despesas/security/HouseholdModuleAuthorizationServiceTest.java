package com.gilrossi.despesas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.identity.HouseholdModuleKey;
import com.gilrossi.despesas.identity.HouseholdModuleService;

@ExtendWith(MockitoExtension.class)
class HouseholdModuleAuthorizationServiceTest {

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Mock
	private HouseholdModuleService householdModuleService;

	@InjectMocks
	private HouseholdModuleAuthorizationService service;

	@Test
	void deve_permitir_acesso_quando_modulo_estiver_habilitado_para_o_espaco_atual() {
		when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedHouseholdUser(
			1L,
			11L,
			"OWNER",
			"Owner",
			"owner@local.invalid",
			"hash",
			Instant.now()
		));
		when(householdModuleService.isEnabled(11L, HouseholdModuleKey.DRIVER)).thenReturn(true);

		assertThat(service.canAccess(HouseholdModuleKey.DRIVER)).isTrue();
	}

	@Test
	void deve_negar_acesso_quando_usuario_nao_tiver_espaco_atual() {
		when(currentUserProvider.requireCurrentUser()).thenReturn(AuthenticatedHouseholdUser.platformAdmin(
			new com.gilrossi.despesas.identity.AppUser(
				"Platform Admin",
				"platform-admin@local.invalid",
				"hash",
				com.gilrossi.despesas.identity.PlatformUserRole.PLATFORM_ADMIN
			)
		));

		assertThat(service.canAccess(HouseholdModuleKey.DRIVER)).isFalse();
	}

	@Test
	void deve_negar_acesso_quando_nao_houver_usuario_autenticado() {
		when(currentUserProvider.requireCurrentUser()).thenThrow(new IllegalStateException("Authenticated household user is required"));

		assertThat(service.canAccess(HouseholdModuleKey.DRIVER)).isFalse();
	}
}
