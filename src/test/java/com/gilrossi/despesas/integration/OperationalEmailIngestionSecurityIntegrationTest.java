package com.gilrossi.despesas.integration;

import static com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport.signedOperationalPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.audit.PersistedAuditEvent;
import com.gilrossi.despesas.audit.PersistedAuditEventRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.ratelimit.RateLimitCounterRepository;
import com.gilrossi.despesas.security.OperationalEmailIngestionAuditLogger;
import com.gilrossi.despesas.security.OperationalRequestSignatureSupport;
import com.gilrossi.despesas.support.ApiAuthTestSupport;
import com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalEmailIngestionSecurityIntegrationTest {

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

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		persistedAuditEventRepository.deleteAll();
		rateLimitCounterRepository.deleteAll();
		Logger logger = (Logger) LoggerFactory.getLogger(OperationalEmailIngestionAuditLogger.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		Logger logger = (Logger) LoggerFactory.getLogger(OperationalEmailIngestionAuditLogger.class);
		logger.detachAppender(logAppender);
	}

	@Test
	void deve_auditar_boundary_operacional_sem_vazar_segredos_ou_payload_bruto() throws Exception {
		registrationService.register(new RegistrationRequest("Owner", "audit-boundary@local.invalid", "senha123", "Casa Audit"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "audit-boundary@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-audit@gmail.com"}
					"""))
			.andExpect(status().isCreated());

		String acceptedPayload = recurringPayload("financeiro-audit@gmail.com", "msg-audit-accepted-1");
		String nonce = UUID.randomUUID().toString();
		long timestamp = Instant.now().getEpochSecond();

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				acceptedPayload,
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				timestamp,
				nonce
			))
			.andExpect(status().isOk());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				acceptedPayload,
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				timestamp,
				nonce
			))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				recurringPayload("financeiro-audit@gmail.com", "msg-audit-invalid-signature"),
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				"segredo-invalido",
				Instant.now().getEpochSecond(),
				UUID.randomUUID().toString()
			))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				recurringPayload("nao-mapeado@gmail.com", "msg-audit-unmapped"),
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				Instant.now().getEpochSecond(),
				UUID.randomUUID().toString()
			))
			.andExpect(status().isUnprocessableEntity());

		List<String> messages = logAppender.list.stream()
			.map(ILoggingEvent::getFormattedMessage)
			.toList();

		assertThat(messages).anyMatch(message -> message.contains("event=operational_request_received"));
		assertThat(messages).anyMatch(message -> message.contains("event=operational_request_rejected") && message.contains("reason=replay_detected"));
		assertThat(messages).anyMatch(message -> message.contains("event=operational_request_rejected") && message.contains("reason=invalid_signature"));
		assertThat(messages).anyMatch(message -> message.contains("event=operational_ingestion_source_rejected"));
		assertThat(messages).anyMatch(message -> message.contains("event=operational_ingestion_accepted"));
		assertThat(messages).noneMatch(message -> message.contains(OperationalRequestSignatureTestSupport.TEST_SECRET));
		assertThat(messages).noneMatch(message -> message.contains("financeiro-audit@gmail.com"));
		assertThat(messages).noneMatch(message -> message.contains("nao-mapeado@gmail.com"));
		assertThat(messages).noneMatch(message -> message.contains("segredo-invalido"));

		List<PersistedAuditEvent> events = persistedAuditEventRepository.findAll();
		assertThat(events).extracting(PersistedAuditEvent::getEventType).contains(
			"operational_request_received",
			"operational_request_rejected",
			"operational_ingestion_source_rejected",
			"operational_ingestion_accepted"
		);
		assertThat(events).anyMatch(event -> "operational_request_rejected".equals(event.getEventType())
			&& "REPLAY_DETECTED".equals(event.getDetailCode()));
		assertThat(events).anyMatch(event -> "operational_request_rejected".equals(event.getEventType())
			&& "INVALID_SIGNATURE".equals(event.getDetailCode()));
		assertThat(events).noneMatch(event -> contains(event.getSafeContextJson(), OperationalRequestSignatureTestSupport.TEST_SECRET));
		assertThat(events).noneMatch(event -> contains(event.getPrimaryReference(), "financeiro-audit@gmail.com"));
		assertThat(events).noneMatch(event -> contains(event.getPrimaryReference(), "nao-mapeado@gmail.com"));
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

	private boolean contains(String value, String fragment) {
		return value != null && fragment != null && value.contains(fragment);
	}
}
