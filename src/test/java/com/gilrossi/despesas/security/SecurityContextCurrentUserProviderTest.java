package com.gilrossi.despesas.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gilrossi.despesas.identity.HouseholdMemberRole;

class SecurityContextCurrentUserProviderTest {

	private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deve_retornar_principal_autenticado_do_security_context() {
		AuthenticatedHouseholdUser principal = new AuthenticatedHouseholdUser(
			7L,
			11L,
			HouseholdMemberRole.OWNER,
			"Ana",
			"ana@local.invalid",
			"{noop}senha"
		);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities())
		);

		AuthenticatedHouseholdUser currentUser = provider.requireCurrentUser();

		assertEquals(7L, currentUser.getUserId());
		assertEquals(11L, currentUser.getHouseholdId());
	}

	@Test
	void deve_lancar_excecao_quando_nao_houver_principal_household_autenticado() {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("anonymousUser", "N/A")
		);

		assertThrows(IllegalStateException.class, provider::requireCurrentUser);
	}
}
