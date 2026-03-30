package com.gilrossi.despesas.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport.signedOperationalPost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionRecordRepository;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.support.ApiAuthTestSupport;
import com.gilrossi.despesas.support.OperationalRequestSignatureTestSupport;
import com.gilrossi.despesas.security.OperationalRequestSignatureSupport;

@SpringBootTest
@AutoConfigureMockMvc
class EmailIngestionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private EmailIngestionRecordRepository recordRepository;

	@Test
	void deve_exigir_assinatura_operacional_valida_para_ingestao() throws Exception {
		mockMvc.perform(post("/api/v1/operations/email-ingestions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRecurringBillPayload("financeiro@gmail.com", "msg-auth-1", "Casa", "Internet", 0.96)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro@gmail.com", "msg-auth-1", "Casa", "Internet", 0.96),
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				"segredo-invalido",
				Instant.now().getEpochSecond(),
				UUID.randomUUID().toString()
			))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void deve_rejeitar_replay_e_timestamp_fora_da_janela() throws Exception {
		registrationService.register(new RegistrationRequest("Helena", "replay-owner@local.invalid", "senha123", "Casa Replay"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "replay-owner@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-replay@gmail.com"}
					"""))
			.andExpect(status().isCreated());

		String payload = validRecurringBillPayload("financeiro-replay@gmail.com", "msg-replay-1", "Casa", "Internet", 0.96);
		String nonce = UUID.randomUUID().toString();
		long currentTimestamp = Instant.now().getEpochSecond();

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				payload,
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				currentTimestamp,
				nonce
			))
			.andExpect(status().isOk());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				payload,
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				currentTimestamp,
				nonce
			))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-replay@gmail.com", "msg-replay-2", "Casa", "Internet", 0.96),
				OperationalRequestSignatureTestSupport.TEST_KEY_ID,
				OperationalRequestSignatureTestSupport.TEST_SECRET,
				currentTimestamp - 3600,
				UUID.randomUUID().toString()
			))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void deve_permitir_source_mapping_para_owner_e_bloquear_member() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "source-owner@local.invalid", "senha123", "Casa da Ana"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "source-owner@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name":"Bia",
					  "email":"source-member@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.role").value("MEMBER"));

		String memberToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "source-member@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-owner@gmail.com",
					  "label":"Gmail da Ana"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.sourceAccount").value("financeiro-owner@gmail.com"));

		mockMvc.perform(get("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].sourceAccount").value("financeiro-owner@gmail.com"));

		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + memberToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-member@gmail.com"
					}
					"""))
			.andExpect(status().isForbidden());

		org.assertj.core.api.Assertions.assertThat(owner.householdId()).isGreaterThanOrEqualTo(1L);
	}

	@Test
	void deve_auto_importar_cobranca_recorrente_de_alta_confianca_sem_itens() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Clara", "ingest-clara@local.invalid", "senha123", "Casa da Clara"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "ingest-clara@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-clara@gmail.com",
					  "label":"Gmail da Clara"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-clara@gmail.com", "msg-recorrente-1", "Casa", "Internet", 0.96)
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.decision").value("AUTO_IMPORTED"))
			.andExpect(jsonPath("$.data.reason").value("IMPORTED"))
			.andExpect(jsonPath("$.data.expenseId").value(greaterThanOrEqualTo(1)));

		org.assertj.core.api.Assertions.assertThat(expenseRepository.findAllByHouseholdId(owner.householdId()))
			.anySatisfy(expense -> {
				org.assertj.core.api.Assertions.assertThat(expense.getDescription()).isEqualTo("Claro Internet");
				org.assertj.core.api.Assertions.assertThat(expense.getAmount()).isEqualByComparingTo("120.00");
			});
	}

	@Test
	void deve_enviar_para_review_quando_confianca_for_media() throws Exception {
		registrationService.register(new RegistrationRequest("Dani", "ingest-dani@local.invalid", "senha123", "Casa da Dani"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "ingest-dani@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-dani@gmail.com"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-dani@gmail.com", "msg-recorrente-review", "Casa", "Internet", 0.72)
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.decision").value("REVIEW_REQUIRED"))
			.andExpect(jsonPath("$.data.reason").value("REVIEW_REQUESTED"));
	}

	@Test
	void deve_ignorar_irrelevante_e_duplicado() throws Exception {
		registrationService.register(new RegistrationRequest("Eva", "ingest-eva@local.invalid", "senha123", "Casa da Eva"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "ingest-eva@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-eva@gmail.com"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				"""
					{
					  "sourceAccount":"financeiro-eva@gmail.com",
					  "externalMessageId":"msg-irrelevante-1",
					  "sender":"newsletter@loja.com",
					  "subject":"Ofertas da semana",
					  "receivedAt":"2026-03-19T10:15:30Z",
					  "currency":"BRL",
					  "classification":"IRRELEVANT",
					  "confidence":0.99,
					  "rawReference":"gmail:msg-irrelevante-1",
					  "desiredDecision":"IGNORE"
					}
					"""
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.decision").value("IGNORED"))
			.andExpect(jsonPath("$.data.reason").value("IRRELEVANT_CLASSIFICATION"));

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-eva@gmail.com", "msg-dup-1", "Casa", "Internet", 0.96)
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.decision").value("AUTO_IMPORTED"));

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-eva@gmail.com", "msg-dup-1", "Casa", "Internet", 0.96)
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.decision").value("IGNORED"))
			.andExpect(jsonPath("$.data.reason").value("DUPLICATE_MESSAGE_ID"))
			.andExpect(jsonPath("$.data.duplicate").value(true));
	}

	@Test
	void deve_importar_compra_manual_pet_shop_com_multiplos_itens() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Fabi", "ingest-fabi@local.invalid", "senha123", "Casa da Fabi"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "ingest-fabi@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-fabi@gmail.com"
					}
					"""))
			.andExpect(status().isCreated());

		Category pets = categoryRepository.save(owner.householdId(), new Category(null, "Pets", true));
		Subcategory petShop = subcategoryRepository.save(owner.householdId(), new Subcategory(null, pets.getId(), "Pet shop", true));

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				"""
					{
					  "sourceAccount":"financeiro-fabi@gmail.com",
					  "externalMessageId":"msg-cobasi-1",
					  "sender":"noreply@cobasi.com.br",
					  "subject":"Compra Cobasi",
					  "receivedAt":"2026-03-19T10:15:30Z",
					  "merchantOrPayee":"Cobasi",
					  "suggestedCategoryName":"Pets",
					  "suggestedSubcategoryName":"Pet shop",
					  "totalAmount":289.70,
					  "occurredOn":"2026-03-19",
					  "currency":"BRL",
					  "items":[
						{"description":"Ração","amount":199.90},
						{"description":"Areia","amount":49.90},
						{"description":"Petisco","amount":39.90}
					  ],
					  "summary":"Compra manual pet shop",
					  "classification":"MANUAL_PURCHASE",
					  "confidence":0.97,
					  "rawReference":"gmail:msg-cobasi-1",
					  "desiredDecision":"AUTO_IMPORT"
					}
					"""
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.decision").value("AUTO_IMPORTED"))
			.andExpect(jsonPath("$.data.expenseId").value(greaterThanOrEqualTo(1)));

		org.assertj.core.api.Assertions.assertThat(expenseRepository.findAllByHouseholdId(owner.householdId()))
			.anySatisfy(expense -> {
				org.assertj.core.api.Assertions.assertThat(expense.getCategoryId()).isEqualTo(pets.getId());
				org.assertj.core.api.Assertions.assertThat(expense.getSubcategoryId()).isEqualTo(petShop.getId());
			});
		org.assertj.core.api.Assertions.assertThat(recordRepository.findAllByHouseholdId(owner.householdId()))
			.anySatisfy(record -> org.assertj.core.api.Assertions.assertThat(record.items()).hasSize(3));
	}

	@Test
	void deve_respeitar_isolamento_por_household_na_source_operacional() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest("Ana", "isolamento-ana@local.invalid", "senha123", "Casa da Ana"));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest("Bruno", "isolamento-bruno@local.invalid", "senha123", "Casa do Bruno"));
		String tokenAna = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "isolamento-ana@local.invalid", "senha123");
		String tokenBruno = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "isolamento-bruno@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + tokenAna)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-ana@gmail.com"}
					"""))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + tokenBruno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-bruno@gmail.com"}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-ana@gmail.com", "msg-isolamento-1", "Casa", "Internet", 0.96)
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.householdId").value(ana.householdId()));

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				validRecurringBillPayload("financeiro-bruno@gmail.com", "msg-isolamento-2", "Casa", "Internet", 0.96)
			))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.householdId").value(bruno.householdId()));

		org.assertj.core.api.Assertions.assertThat(expenseRepository.findAllByHouseholdId(ana.householdId())).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(expenseRepository.findAllByHouseholdId(bruno.householdId())).hasSize(1);
	}

	@Test
	void deve_validar_payload_operacional_invalido() throws Exception {
		registrationService.register(new RegistrationRequest("Gabi", "ingest-gabi@local.invalid", "senha123", "Casa da Gabi"));
		String ownerToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "ingest-gabi@local.invalid", "senha123");
		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro-gabi@gmail.com"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				"""
					{
					  "sourceAccount":"financeiro-gabi@gmail.com",
					  "externalMessageId":"msg-invalido-1",
					  "sender":" ",
					  "subject":"Compra Cobasi",
					  "receivedAt":"2026-03-19T10:15:30Z",
					  "currency":"BRL",
					  "classification":"MANUAL_PURCHASE",
					  "confidence":0.97,
					  "rawReference":"gmail:msg-invalido-1",
					  "desiredDecision":"AUTO_IMPORT"
					}
					"""
			))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void deve_rejeitar_payload_que_tenta_forcar_household_externo() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest("Ana", "payload-ana@local.invalid", "senha123", "Casa da Ana"));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest("Bruno", "payload-bruno@local.invalid", "senha123", "Casa do Bruno"));
		String tokenAna = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "payload-ana@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/email-ingestion/sources")
				.header("Authorization", "Bearer " + tokenAna)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"sourceAccount":"financeiro-payload@gmail.com"}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(signedOperationalPost(
				"/api/v1/operations/email-ingestions",
				"""
					{
					  "householdId": %d,
					  "sourceAccount":"financeiro-payload@gmail.com",
					  "externalMessageId":"msg-household-forcado-1",
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
					  "rawReference":"gmail:msg-household-forcado-1",
					  "desiredDecision":"AUTO_IMPORT"
					}
					""".formatted(bruno.householdId())
			))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"));

		org.assertj.core.api.Assertions.assertThat(expenseRepository.findAllByHouseholdId(ana.householdId())).isEmpty();
		org.assertj.core.api.Assertions.assertThat(expenseRepository.findAllByHouseholdId(bruno.householdId())).isEmpty();
	}

	private String validRecurringBillPayload(String sourceAccount, String messageId, String categoryName, String subcategoryName, double confidence) {
		return String.format(Locale.US, """
			{
			  "sourceAccount":"%s",
			  "externalMessageId":"%s",
			  "sender":"conta@claro.com.br",
			  "subject":"Claro Internet março",
			  "receivedAt":"2026-03-19T10:15:30Z",
			  "merchantOrPayee":"Claro Internet",
			  "suggestedCategoryName":"%s",
			  "suggestedSubcategoryName":"%s",
			  "totalAmount":120.00,
			  "dueDate":"2026-03-25",
			  "currency":"BRL",
			  "summary":"Cobrança recorrente mensal",
			  "classification":"RECURRING_BILL",
			  "confidence":%.2f,
			  "rawReference":"gmail:%s",
			  "desiredDecision":"AUTO_IMPORT"
			}
			""", sourceAccount, messageId, categoryName, subcategoryName, confidence, messageId);
	}
}
