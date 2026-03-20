package com.gilrossi.despesas.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public final class ApiAuthTestSupport {

	private ApiAuthTestSupport() {
	}

	public static String accessToken(MockMvc mockMvc, ObjectMapper objectMapper, String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"%s",
					  "password":"%s"
					}
					""".formatted(email, password)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("accessToken").asText();
	}
}
