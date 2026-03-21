package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.gilrossi.despesas.support.ApiAuthTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class EmailIngestionReviewIntegrationTest {

	private static final String OPERATIONS_TOKEN = "test-operational-email-ingestion-token";

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
	void deve_listar_apenas_pendencias_review_required_na_tela() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-list-owner@local.invalid", "financeiro-list@gmail.com");
		createOperationalCandidate("financeiro-list@gmail.com", "msg-review-list-1", 0.72);
		createOperationalCandidate("financeiro-list@gmail.com", "msg-review-list-2", 0.96);

		mockMvc.perform(get("/revisoes")
				.with(user(ownerPrincipal(owner))))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Claro Internet msg-review-list-1")))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Claro Internet msg-review-list-2"))));

		assertThat(recordRepository.findAllPendingReviewByHouseholdId(owner.householdId())).hasSize(1);
	}

	@Test
	void deve_aprovar_pendencia_e_importar_despesa_no_household_correto() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-approve-owner@local.invalid", "financeiro-approve@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-approve@gmail.com", "msg-review-approve-1", 0.72);

		mockMvc.perform(post("/revisoes/{id}/aprovar", ingestionId)
				.with(user(ownerPrincipal(owner)))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attributeExists("mensagemSucesso"));

		EmailIngestionRecord updated = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(updated.finalDecision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
		assertThat(updated.decisionReason()).isEqualTo(EmailIngestionDecisionReason.MANUALLY_IMPORTED);
		assertThat(updated.importedExpenseId()).isNotNull();
		assertThat(expenseRepository.findAllByHouseholdId(owner.householdId()))
			.anySatisfy(expense -> assertThat(expense.getId()).isEqualTo(updated.importedExpenseId()));
	}

	@Test
	void deve_rejeitar_pendencia_sem_importar_despesa() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-reject-owner@local.invalid", "financeiro-reject@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-reject@gmail.com", "msg-review-reject-1", 0.72);

		mockMvc.perform(post("/revisoes/{id}/rejeitar", ingestionId)
				.with(user(ownerPrincipal(owner)))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attributeExists("mensagemSucesso"));

		EmailIngestionRecord updated = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(updated.finalDecision()).isEqualTo(EmailIngestionFinalDecision.IGNORED);
		assertThat(updated.decisionReason()).isEqualTo(EmailIngestionDecisionReason.MANUALLY_REJECTED);
		assertThat(updated.importedExpenseId()).isNull();
		assertThat(expenseRepository.findAllByHouseholdId(owner.householdId())).isEmpty();
	}

	@Test
	void deve_bloquear_transicao_invalida_e_preservar_estado() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-invalid-owner@local.invalid", "financeiro-invalid@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-invalid@gmail.com", "msg-review-invalid-1", 0.96);

		mockMvc.perform(post("/revisoes/{id}/aprovar", ingestionId)
				.with(user(ownerPrincipal(owner)))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attribute("mensagemErro", "A ingestão selecionada não está mais pendente de revisão."));

		EmailIngestionRecord unchanged = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(unchanged.finalDecision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
		assertThat(expenseRepository.findAllByHouseholdId(owner.householdId())).hasSize(1);
	}

	@Test
	void deve_respeitar_isolamento_por_household_na_aprovacao() throws Exception {
		RegistrationResponse ana = registerAndMapSource("review-isolation-ana@local.invalid", "financeiro-ana-review@gmail.com");
		RegistrationResponse bruno = registerAndMapSource("review-isolation-bruno@local.invalid", "financeiro-bruno-review@gmail.com");
		Long ingestionId = createOperationalCandidate("financeiro-ana-review@gmail.com", "msg-review-isolation-1", 0.72);

		mockMvc.perform(post("/revisoes/{id}/aprovar", ingestionId)
				.with(user(ownerPrincipal(bruno)))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attribute("mensagemErro", "Ingestão #" + ingestionId + " não foi encontrada para o household atual."));

		EmailIngestionRecord unchanged = recordRepository.findByIdAndHouseholdId(ingestionId, ana.householdId()).orElseThrow();
		assertThat(unchanged.finalDecision()).isEqualTo(EmailIngestionFinalDecision.REVIEW_REQUIRED);
		assertThat(expenseRepository.findAllByHouseholdId(bruno.householdId())).isEmpty();
	}

	@Test
	void deve_aprovar_pendencia_manual_com_catalogo_padrao_geral() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-default-owner@local.invalid", "financeiro-default@gmail.com");
		Long ingestionId = createOperationalCandidate("""
			{
			  "sourceAccount":"financeiro-default@gmail.com",
			  "externalMessageId":"msg-review-default-1",
			  "sender":"manual@local.invalid",
			  "subject":"Compra Cobasi manual",
			  "receivedAt":"2026-03-19T10:15:30Z",
			  "merchantOrPayee":"Cobasi",
			  "suggestedCategoryName":"Geral",
			  "suggestedSubcategoryName":"Primeiros lançamentos",
			  "totalAmount":20.00,
			  "occurredOn":"2026-03-19",
			  "currency":"BRL",
			  "items":[
			    {"description":"Ração","amount":10.00},
			    {"description":"Brinquedo","amount":10.00}
			  ],
			  "summary":"Compra manual detectada",
			  "classification":"MANUAL_PURCHASE",
			  "confidence":0.88,
			  "rawReference":"gmail:msg-review-default-1",
			  "desiredDecision":"REVIEW"
			}
			""");

		mockMvc.perform(post("/revisoes/{id}/aprovar", ingestionId)
				.with(user(ownerPrincipal(owner)))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attributeExists("mensagemSucesso"));

		EmailIngestionRecord updated = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(updated.finalDecision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
		assertThat(updated.decisionReason()).isEqualTo(EmailIngestionDecisionReason.MANUALLY_IMPORTED);
		assertThat(updated.importedExpenseId()).isNotNull();
		assertThat(expenseRepository.findAllByHouseholdId(owner.householdId()))
			.anySatisfy(expense -> {
				assertThat(expense.getId()).isEqualTo(updated.importedExpenseId());
				assertThat(expense.getDescription()).isEqualTo("Cobasi");
				assertThat(expense.getAmount()).isEqualByComparingTo("20.00");
			});
	}

	@Test
	void deve_manter_pendente_quando_moeda_nao_for_suportada_mesmo_com_catalogo_resolvido() throws Exception {
		RegistrationResponse owner = registerAndMapSource("review-usd-owner@local.invalid", "financeiro-usd@gmail.com");
		Long ingestionId = createOperationalCandidate("""
			{
			  "sourceAccount":"financeiro-usd@gmail.com",
			  "externalMessageId":"msg-review-usd-1",
			  "sender":"manual@local.invalid",
			  "subject":"Compra Cobasi USD",
			  "receivedAt":"2026-03-19T10:15:30Z",
			  "merchantOrPayee":"Cobasi",
			  "suggestedCategoryName":"Geral",
			  "suggestedSubcategoryName":"Primeiros lançamentos",
			  "totalAmount":20.00,
			  "occurredOn":"2026-03-19",
			  "currency":"USD",
			  "items":[
			    {"description":"Ração","amount":10.00},
			    {"description":"Brinquedo","amount":10.00}
			  ],
			  "summary":"Compra manual em moeda estrangeira",
			  "classification":"MANUAL_PURCHASE",
			  "confidence":0.88,
			  "rawReference":"gmail:msg-review-usd-1",
			  "desiredDecision":"REVIEW"
			}
			""");

		mockMvc.perform(post("/revisoes/{id}/aprovar", ingestionId)
				.with(user(ownerPrincipal(owner)))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attribute("mensagemErro", "Esta ingestão ainda não pode ser aprovada porque a moeda não é suportada."));

		EmailIngestionRecord unchanged = recordRepository.findByIdAndHouseholdId(ingestionId, owner.householdId()).orElseThrow();
		assertThat(unchanged.finalDecision()).isEqualTo(EmailIngestionFinalDecision.REVIEW_REQUIRED);
		assertThat(unchanged.importedExpenseId()).isNull();
		assertThat(expenseRepository.findAllByHouseholdId(owner.householdId())).isEmpty();
	}

	private RegistrationResponse registerAndMapSource(String email, String sourceAccount) throws Exception {
		RegistrationResponse registration = registrationService.register(new RegistrationRequest("Owner", email, "senha123", "Casa " + email));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, email, "senha123");
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

	private Long createOperationalCandidate(String payload) throws Exception {
		String response = mockMvc.perform(post("/api/v1/operations/email-ingestions")
				.header("Authorization", "Bearer " + OPERATIONS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("ingestionId").asLong();
	}

	private Long createOperationalCandidate(String sourceAccount, String messageId, double confidence) throws Exception {
		String response = mockMvc.perform(post("/api/v1/operations/email-ingestions")
				.header("Authorization", "Bearer " + OPERATIONS_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRecurringBillPayload(sourceAccount, messageId, confidence)))
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
}
