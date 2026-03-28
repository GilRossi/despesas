package com.gilrossi.despesas.api.v1.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.identity.AuthResponse;
import com.gilrossi.despesas.identity.OnboardingStatusResponse;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;
import com.gilrossi.despesas.security.CurrentUserProvider;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CurrentUserProvider currentUserProvider;

	@MockitoBean
	private MobileAuthService mobileAuthService;

	@Test
	void deve_autenticar_login_mobile_e_retornar_tokens() throws Exception {
		when(mobileAuthService.login(any())).thenReturn(new MobileAuthResponse(
			"Bearer",
			"access-token",
			Instant.parse("2026-03-20T04:00:00Z"),
			"refresh-token",
			Instant.parse("2026-04-19T04:00:00Z"),
			new AuthResponse(
				1L,
				10L,
				"ana@local.invalid",
				"Ana",
				"OWNER",
				new OnboardingStatusResponse(false, null)
			)
		));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"ana@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.data.user.email").value("ana@local.invalid"))
			.andExpect(jsonPath("$.data.user.onboarding.completed").value(false));
	}

	@Test
	void deve_validar_payload_de_login_mobile() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"email-invalido",
					  "password":""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("Request validation failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").exists());
	}

	@Test
	void deve_renovar_token_mobile() throws Exception {
		when(mobileAuthService.refresh(any())).thenReturn(new MobileAuthResponse(
			"Bearer",
			"novo-access-token",
			Instant.parse("2026-03-20T04:00:00Z"),
			"novo-refresh-token",
			Instant.parse("2026-04-19T04:00:00Z"),
			new AuthResponse(
				1L,
				10L,
				"ana@local.invalid",
				"Ana",
				"OWNER",
				new OnboardingStatusResponse(true, Instant.parse("2026-03-19T10:15:30Z"))
			)
		));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken":"refresh-token"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("novo-access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("novo-refresh-token"))
			.andExpect(jsonPath("$.data.user.onboarding.completed").value(true))
			.andExpect(jsonPath("$.data.user.onboarding.completedAt").value("2026-03-19T10:15:30Z"));
	}

	@Test
	void deve_retornar_usuario_corrente() throws Exception {
		when(currentUserProvider.requireCurrentUser()).thenReturn(
			new AuthenticatedHouseholdUser(1L, 10L, "OWNER", "Ana", "ana@local.invalid", "{noop}senha123", Instant.now())
		);
		when(mobileAuthService.currentUser(any())).thenReturn(new AuthResponse(
			1L,
			10L,
			"ana@local.invalid",
			"Ana",
			"OWNER",
			new OnboardingStatusResponse(false, null)
		));

		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("ana@local.invalid"))
			.andExpect(jsonPath("$.data.householdId").value(10))
			.andExpect(jsonPath("$.data.onboarding.completed").value(false));
	}

	@Test
	void deve_trocar_senha_do_usuario_corrente() throws Exception {
		when(currentUserProvider.requireCurrentUser()).thenReturn(
			new AuthenticatedHouseholdUser(1L, 10L, "OWNER", "Ana", "ana@local.invalid", "{noop}senha123", Instant.now())
		);
		when(mobileAuthService.changePassword(any(), any()))
			.thenReturn(new ChangePasswordResponse(1, true));

		mockMvc.perform(post("/api/v1/auth/change-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "currentPassword":"senha123",
					  "newPassword":"novaSenha123",
					  "newPasswordConfirmation":"novaSenha123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.revokedRefreshTokens").value(1))
			.andExpect(jsonPath("$.data.reauthenticationRequired").value(true));
	}
}
