package com.gilrossi.despesas.integration;

import static com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport.signedOperationalPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"app.operational.email-ingestion.previous-key-id=test-operational-key-previous",
	"app.operational.email-ingestion.previous-secret=test-operational-secret-previous"
})
class OperationalEmailIngestionKeyRotationIntegrationTest {

	private static final String PREVIOUS_KEY_ID = "test-operational-key-previous";
	private static final String PREVIOUS_SECRET = "test-operational-secret-previous";

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
	void deve_aceitar_assinatura_com_chave_anterior_durante_rotacao() throws Exception {
		registrationService.register(new RegistrationRequest("Owner", "operational-previous-key@local.invalid", "senha123", "Casa Previous Key"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "operational-previous-key@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-previous@gmail.com"}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				recurringPayload("financeiro-previous@gmail.com", "msg-previous-key-1"),
				PREVIOUS_KEY_ID,
				PREVIOUS_SECRET,
				Instant.now().getEpochSecond(),
				UUID.randomUUID().toString()
			))
			.andExpect(status().isOk());

		assertThat(persistedAuditEventRepository.findAll())
			.anyMatch(event -> "operational_request_received".equals(event.getEventType())
				&& PREVIOUS_KEY_ID.equals(event.getSourceKey()));
		assertThat(persistedAuditEventRepository.findAll())
			.extracting(PersistedAuditEvent::getEventType)
			.contains("operational_ingestion_accepted");
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
			  "suggestedCategoryName":"Casa",
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
