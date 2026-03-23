package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport.signedOperationalPost;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionRecord;
import com.gilrossi.despesas.emailingestion.EmailIngestionRecordRepository;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.security.AuthenticatedHouseholdUser;

@SpringBootTest
@AutoConfigureMockMvc
class EmailIngestionReviewApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private EmailIngestionRecordRepository recordRepository;

	@Test
	void deve_listar_pendencias_via_api_para_owner() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-list-owner@local.invalid", "financeiro-api-list@gmail.com");
		createOperationalCandidate("financeiro-api-list@gmail.com", "msg-review-api-list-1", 0.72);
		createOperationalCandidate("financeiro-api-list@gmail.com", "msg-review-api-list-2", 0.96);

		mockMvc.perform(get("/api/v1/email-ingestion/reviews")
				.param("status", "PENDING")
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].subject").value("Claro Internet msg-review-api-list-1"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void deve_detalhar_pendencia_valida_do_household_atual() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-detail-owner@local.invalid", "financeiro-api-detail@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-detail@gmail.com", "msg-review-api-detail-1", 0.72);

		mockMvc.perform(get("/api/v1/email-ingestion/reviews/{id}", ingestionId)
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(ingestionId))
			.andExpect(jsonPath("$.data.finalDecision").value("REVIEW_REQUIRED"))
			.andExpect(jsonPath("$.data.items").isArray());
	}

	@Test
	void deve_aprovar_pendencia_via_api() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-approve-owner@local.invalid", "financeiro-api-approve@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-approve@gmail.com", "msg-review-api-approve-1", 0.72);

		mockMvc.perform(post("/api/v1/email-ingestion/reviews/{id}/approve", ingestionId)
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(ingestionId))
			.andExpect(jsonPath("$.data.decision").value("AUTO_IMPORTED"))
			.andExpect(jsonPath("$.data.expenseId").isNumber());

		EmailIngestionRecord updated = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(updated.finalDecision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
		assertThat(updated.importedExpenseId()).isNotNull();
	}

	@Test
	void deve_rejeitar_pendencia_via_api() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-reject-owner@local.invalid", "financeiro-api-reject@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-reject@gmail.com", "msg-review-api-reject-1", 0.72);

		mockMvc.perform(post("/api/v1/email-ingestion/reviews/{id}/reject", ingestionId)
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(ingestionId))
			.andExpect(jsonPath("$.data.decision").value("IGNORED"));

		EmailIngestionRecord updated = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(updated.finalDecision()).isEqualTo(EmailIngestionFinalDecision.IGNORED);
		assertThat(updated.importedExpenseId()).isNull();
	}

	@Test
	void deve_retornar_404_para_item_inexistente() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-missing-owner@local.invalid", "financeiro-api-missing@gmail.com");

		mockMvc.perform(get("/api/v1/email-ingestion/reviews/999999")
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void deve_retornar_422_para_item_ja_resolvido() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-resolved-owner@local.invalid", "financeiro-api-resolved@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-resolved@gmail.com", "msg-review-api-resolved-1", 0.96);

		mockMvc.perform(post("/api/v1/email-ingestion/reviews/{id}/approve", ingestionId)
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isUnprocessableEntity());

		EmailIngestionRecord unchanged = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(unchanged.finalDecision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
	}

	@Test
	void deve_bloquear_acesso_sem_papel_owner() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-role-owner@local.invalid", "financeiro-api-role@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-role@gmail.com", "msg-review-api-role-1", 0.72);

		mockMvc.perform(get("/api/v1/email-ingestion/reviews/{id}", ingestionId)
				.with(user(memberPrincipal(owner))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void deve_respeitar_boundary_do_household_na_aprovacao() throws Exception {
		RegistrationResponse ana = registerAndMapSource("review-api-boundary-ana@local.invalid", "financeiro-api-boundary-ana@gmail.com");
		RegistrationResponse bruno = registerAndMapSource("review-api-boundary-bruno@local.invalid", "financeiro-api-boundary-bruno@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-boundary-ana@gmail.com", "msg-review-api-boundary-1", 0.72);

		mockMvc.perform(post("/api/v1/email-ingestion/reviews/{id}/approve", ingestionId)
				.with(user(ownerPrincipal(bruno))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));

		EmailIngestionRecord unchanged = recordRepository.findByIdAndHouseholdId(ingestionId, ana.householdId()).orElseThrow();
		assertThat(unchanged.finalDecision()).isEqualTo(EmailIngestionFinalDecision.REVIEW_REQUIRED);
		assertThat(expenseRepository.findAllByHouseholdId(bruno.householdId())).isEmpty();
	}

	@Test
	void deve_permitir_apenas_uma_decisao_concorrente_para_mesma_pendencia() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-api-race-owner@local.invalid", "financeiro-api-race@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-api-race@gmail.com", "msg-review-api-race-1", 0.72);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<Integer> approve = executor.submit(() -> performConcurrentDecision(owner, ingestionId, true, ready, start));
			Future<Integer> reject = executor.submit(() -> performConcurrentDecision(owner, ingestionId, false, ready, start));
			ready.await();
			start.countDown();
			List<Integer> statuses = List.of(approve.get(), reject.get());
			assertThat(statuses).containsExactlyInAnyOrder(200, 422);
		}

		EmailIngestionRecord updated = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(updated.finalDecision()).isIn(EmailIngestionFinalDecision.AUTO_IMPORTED, EmailIngestionFinalDecision.IGNORED);
		if (updated.finalDecision() == EmailIngestionFinalDecision.AUTO_IMPORTED) {
			assertThat(updated.importedExpenseId()).isNotNull();
		}
	}

	private RegistrationResponse registerAndMapSource(String email, String sourceAccount) throws Exception {
		RegistrationResponse registration = registrationService.register(new RegistrationRequest("Owner", email, "senha123", "Casa " + email));
		String ownerToken = accessToken(email, "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"%s"
					}
					""".formatted(sourceAccount)))
			.andExpect(status().isCreated());
		return registration;
	}

	private String accessToken(String email, String password) throws Exception {
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

	private Long createOperationalCandidate(String sourceAccount, String messageId, double confidence) throws Exception {
		String response = mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload(sourceAccount, messageId, confidence)
			))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("ingestionId").asLong();
	}

	private String validRecurringBillPayload(String sourceAccount, String messageId, double confidence) {
		return String.format(Locale.US, """
			{
			  "sourceAccount":"%s",
			  "externalMessageId":"%s",
			  "sender":"conta@claro.com.br",
			  "subject":"Claro Internet %s",
			  "receivedAt":"2026-03-19T10:15:30Z",
			  "merchantOrPayee":"Claro Internet",
			  "suggestedCategoryName":"Casa",
			  "suggestedSubcategoryName":"Internet",
			  "totalAmount":120.00,
			  "dueDate":"2026-03-25",
			  "currency":"BRL",
			  "summary":"Cobrança recorrente mensal",
			  "classification":"RECURRING_BILL",
			  "confidence":%.2f,
			  "rawReference":"gmail:%s",
			  "desiredDecision":"AUTO_IMPORT"
			}
			""", sourceAccount, messageId, messageId, confidence, messageId);
	}

	private Integer performConcurrentDecision(
		RegistrationResponse owner,
		Long ingestionId,
		boolean approve,
		CountDownLatch ready,
		CountDownLatch start
	) throws Exception {
		ready.countDown();
		start.await();
		return mockMvc.perform(post("/api/v1/email-ingestion/reviews/{id}/{action}", ingestionId, approve ? "approve" : "reject")
				.with(user(ownerPrincipal(owner))))
			.andReturn()
			.getResponse()
			.getStatus();
	}

	private AuthenticatedHouseholdUser ownerPrincipal(RegistrationResponse registration) {
		return new AuthenticatedHouseholdUser(
			registration.userId(),
			registration.householdId(),
			HouseholdMemberRole.OWNER,
			registration.name(),
			registration.email(),
			"senha123"
		);
	}

	private AuthenticatedHouseholdUser memberPrincipal(RegistrationResponse registration) {
		return new AuthenticatedHouseholdUser(
			registration.userId(),
			registration.householdId(),
			HouseholdMemberRole.MEMBER,
			registration.name(),
			registration.email(),
			"senha123"
		);
	}
}
