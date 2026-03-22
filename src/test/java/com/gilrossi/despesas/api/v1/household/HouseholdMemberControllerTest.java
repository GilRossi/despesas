package com.gilrossi.despesas.api.v1.household;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.identity.HouseholdMemberResponse;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.HouseholdMemberService;

@WebMvcTest(HouseholdMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class HouseholdMemberControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HouseholdMemberService service;

	@Test
	void deve_listar_membros_do_household() throws Exception {
		when(service.listMembers()).thenReturn(List.of(
			new HouseholdMemberResponse(1L, 2L, 10L, "Ana", "ana@local.invalid", HouseholdMemberRole.OWNER),
			new HouseholdMemberResponse(3L, 4L, 10L, "Bia", "bia@local.invalid", HouseholdMemberRole.MEMBER)
		));

		mockMvc.perform(get("/api/v1/household/members"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].email").value("ana@local.invalid"))
			.andExpect(jsonPath("$.data[1].role").value("MEMBER"));
	}

	@Test
	void deve_criar_membro_e_retornar_201() throws Exception {
		when(service.create(any())).thenReturn(
			new HouseholdMemberResponse(1L, 2L, 10L, "Bia", "bia@local.invalid", HouseholdMemberRole.MEMBER)
		);

		mockMvc.perform(post("/api/v1/household/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"name":"Bia",
						"email":"bia@local.invalid",
						"password":"senha123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.email").value("bia@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("MEMBER"));

		verify(service).create(any());
	}

	@Test
	void deve_validar_payload_invalido_ao_criar_membro() throws Exception {
		mockMvc.perform(post("/api/v1/household/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"name":"",
						"email":"email-invalido",
						"password":"123"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
