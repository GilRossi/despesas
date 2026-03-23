package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;
import com.gilrossi.despesas.security.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
class FinancialAssistantAccessContextProviderTest {

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Test
	void deve_retornar_contexto_com_usuario_e_household_para_member_do_household() {
		when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedHouseholdUser(
			7L,
			11L,
			HouseholdMemberRole.OWNER,
			"Ana",
			"ana@local.invalid",
			"{noop}senha"
		));

		FinancialAssistantAccessContextProvider provider = new FinancialAssistantAccessContextProvider(currentUserProvider);

		FinancialAssistantAccessContext context = provider.requireContext();
		assertThat(context.userId()).isEqualTo(7L);
		assertThat(context.householdId()).isEqualTo(11L);
		assertThat(context.role()).isEqualTo("OWNER");
	}

	@Test
	void deve_rejeitar_platform_admin_sem_household_ativo() {
		when(currentUserProvider.requireCurrentUser()).thenReturn(new AuthenticatedHouseholdUser(
			9L,
			null,
			"PLATFORM_ADMIN",
			"Admin",
			"admin@local.invalid",
			"{noop}senha"
		));

		FinancialAssistantAccessContextProvider provider = new FinancialAssistantAccessContextProvider(currentUserProvider);

		FinancialAssistantContextException exception = assertThrows(FinancialAssistantContextException.class, provider::requireContext);
		assertThat(exception.reasonCode()).isEqualTo("ASSISTANT_INVALID_HOUSEHOLD_CONTEXT");
		assertThat(exception.userId()).isEqualTo(9L);
		assertThat(exception.role()).isEqualTo("PLATFORM_ADMIN");
	}
}
