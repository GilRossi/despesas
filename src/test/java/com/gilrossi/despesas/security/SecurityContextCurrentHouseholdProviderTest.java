package com.gilrossi.despesas.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.identity.HouseholdMemberRole;

@ExtendWith(MockitoExtension.class)
class SecurityContextCurrentHouseholdProviderTest {

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Test
	void deve_retornar_household_id_do_usuario_atual() {
		when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedHouseholdUser(
			7L,
			11L,
			HouseholdMemberRole.OWNER,
			"Ana",
			"ana@local.invalid",
			"{noop}senha",
			Instant.now()
		));

		SecurityContextCurrentHouseholdProvider provider = new SecurityContextCurrentHouseholdProvider(currentUserProvider);

		assertEquals(11L, provider.requireHouseholdId());
	}

	@Test
	void deve_rejeitar_usuario_sem_household_quando_fluxo_exigir_household() {
		when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedHouseholdUser(
			7L,
			null,
			"PLATFORM_ADMIN",
			"Admin",
			"admin@local.invalid",
			"{noop}senha",
			Instant.now()
		));

		SecurityContextCurrentHouseholdProvider provider = new SecurityContextCurrentHouseholdProvider(currentUserProvider);

		assertThrows(IllegalStateException.class, provider::requireHouseholdId);
	}
}
