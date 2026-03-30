package com.gilrossi.despesas.integration;

import static com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport.signedOperationalPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.audit.PersistedAuditEvent;
import com.gilrossi.despesas.audit.PersistedAuditEventRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.ratelimit.RateLimitCounterRepository;
import com.gilrossi.despesas.support.ApiAuthTestSupport;
import com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"app.rate-limits.operational-email-ingestion.max-requests=1",
	"app.rate-limits.operational-email-ingestion.window-seconds=300"
})
class OperationalEmailIngestionRateLimitIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PersistedAuditEventRepository persistedAuditEventRepository;

	@Autowired
	private RateLimitCounterRepository rateLimitCounterRepository;

	@BeforeEach
	void setUp() {
		persistedAuditEventRepository.deleteAll();
		rateLimitCounterRepository.deleteAll();
	}

	@Test
	void deve_limitar_boundary_operacional_e_persistir_evento_de_abuso() throws Exception {
		registrationService.register(new RegistrationRequest("Owner", "operational-rate@local.invalid", "senha123", "Casa Operacional"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "operational-rate@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-rate@gmail.com"}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				recurringPayload("financeiro-rate@gmail.com", "msg-rate-1"),
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				Instant.now().getEpochSecond(),
				UUID.randomUUID().toString()
			))
			.andExpect(status().isOk());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				recurringPayload("financeiro-rate@gmail.com", "msg-rate-2"),
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				Instant.now().getEpochSecond(),
				UUID.randomUUID().toString()
			))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		assertThat(persistedAuditEventRepository.findAll())
			.extracting(PersistedAuditEvent::getEventType)
			.contains("operational_request_received", "operational_request_rate_limited");

		PersistedAuditEvent limitedEvent = persistedAuditEventRepository.findAllByEventTypeOrderByIdAsc("operational_request_rate_limited").get(0);
		assertThat(limitedEvent.getSourceKey()).isEqualTo(OperationalRequestSignatureTestSupport.TEST_KEY_ID);
		assertThat(limitedEvent.getSafeContextJson()).doesNotContain("financeiro-rate@gmail.com").doesNotContain("msg-rate-2");
	}

	private String recurringPayload(String sourceAccount, String messageId) {
		return """
			{
			  "sourceAccount":"%s",
			  "externalMessageId":"%s",
			  "sender":"conta@claro.com.br",
			  "subject":"Claro Internet março",
			  "receivedAt":"2026-03-19T10:15:30Z",
			  "merchantOrPayee":"Claro Internet",
			  "suggestedCategoryName":"Moradia",
			  "suggestedSubcategoryName":"Internet",
			  "totalAmount":120.00,
			  "dueDate":"2026-03-25",
			  "currency":"BRL",
			  "summary":"Cobrança recorrente mensal",
			  "classification":"RECURRING_BILL",
			  "confidence":0.96,
			  "rawReference":"gmail:%s",
			  "desiredDecision":"AUTO_IMPORT"
			}
			""".formatted(sourceAccount, messageId, messageId);
	}
}
