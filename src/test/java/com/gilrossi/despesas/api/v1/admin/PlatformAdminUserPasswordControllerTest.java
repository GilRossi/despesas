package com.gilrossi.despesas.api.v1.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;
import com.gilrossi.despesas.security.CurrentUserProvider;
import com.gilrossi.despesas.security.PasswordManagementService;

import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest(PlatformAdminUserPasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PlatformAdminUserPasswordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CurrentUserProvider currentUserProvider;

	@MockitoBean
	private PasswordManagementService passwordManagementService;

	@Test
	void deve_resetar_senha_via_endpoint_admin() throws Exception {
		when(currentUserProvider.requireCurrentUser()).thenReturn(
			new AuthenticatedHouseholdUser(
				1L,
				null,
				"PLATFORM_ADMIN",
				"Admin",
				"admin@local.invalid",
				"{noop}senha123",
				Instant.now()
			)
		);
		when(passwordManagementService.resetPassword(any(), any()))
			.thenReturn(new AdminPasswordResetResponse("u***@local.invalid", 2));

		mockMvc.perform(post("/api/v1/admin/users/password-reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetEmail":"user@local.invalid",
					  "newPassword":"novaSenha123",
					  "newPasswordConfirmation":"novaSenha123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.targetEmailMasked").value("u***@local.invalid"))
			.andExpect(jsonPath("$.data.revokedRefreshTokens").value(2));
	}
}
