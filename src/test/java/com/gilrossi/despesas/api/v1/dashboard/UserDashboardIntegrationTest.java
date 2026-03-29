package com.gilrossi.despesas.api.v1.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.HouseholdRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class UserDashboardIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private HouseholdRepository householdRepository;

	@Autowired
	private HouseholdMemberRepository householdMemberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void deve_retornar_dashboard_completo_para_owner() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "owner-dashboard-v2@local.invalid", "senha123", "Casa Dashboard"));
		Category moradia = categoryRepository.save(owner.householdId(), new Category(null, "Moradia", true));
		Category transporte = categoryRepository.save(owner.householdId(), new Category(null, "Transporte", true));
		Subcategory internet = subcategoryRepository.save(owner.householdId(), new Subcategory(null, moradia.getId(), "Internet", true));
		Subcategory aluguel = subcategoryRepository.save(owner.householdId(), new Subcategory(null, moradia.getId(), "Aluguel", true));
		Subcategory combustivel = subcategoryRepository.save(owner.householdId(), new Subcategory(null, transporte.getId(), "Combustível", true));

		Expense vencida = expenseRepository.save(new Expense(
			owner.householdId(),
			"Internet",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(2),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			internet.getId(),
			internet.getName(),
			null
		));
		Expense aluguelAtual = expenseRepository.save(new Expense(
			owner.householdId(),
			"Aluguel",
			new BigDecimal("800.00"),
			LocalDate.now(),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			aluguel.getId(),
			aluguel.getName(),
			null
		));
		Expense combustivelAtual = expenseRepository.save(new Expense(
			owner.householdId(),
			"Combustível",
			new BigDecimal("200.00"),
			LocalDate.now().plusDays(5),
			ExpenseContext.VEICULO,
			transporte.getId(),
			transporte.getName(),
			combustivel.getId(),
			combustivel.getName(),
			null
		));
		Expense despesaMesAnterior = expenseRepository.save(new Expense(
			owner.householdId(),
			"Mercado anterior",
			new BigDecimal("300.00"),
			LocalDate.now().minusMonths(1).withDayOfMonth(10),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			aluguel.getId(),
			aluguel.getName(),
			null
		));

		vencida.setCreatedAt(Instant.parse("2026-03-29T10:00:00Z"));
		aluguelAtual.setCreatedAt(Instant.parse("2026-03-29T11:00:00Z"));
		combustivelAtual.setCreatedAt(Instant.parse("2026-03-29T09:00:00Z"));
		despesaMesAnterior.setCreatedAt(Instant.parse("2026-02-15T08:00:00Z"));
		expenseRepository.save(vencida);
		expenseRepository.save(aluguelAtual);
		expenseRepository.save(combustivelAtual);
		expenseRepository.save(despesaMesAnterior);

		Payment pagamentoAtual = paymentRepository.save(new Payment(
			aluguelAtual.getId(),
			new BigDecimal("500.00"),
			LocalDate.now(),
			PaymentMethod.PIX,
			"Pagamento parcial"
		));
		pagamentoAtual.setCreatedAt(Instant.parse("2026-03-29T12:00:00Z"));
		paymentRepository.save(pagamentoAtual);

		paymentRepository.save(new Payment(
			despesaMesAnterior.getId(),
			new BigDecimal("300.00"),
			LocalDate.now().minusMonths(1).withDayOfMonth(12),
			PaymentMethod.PIX,
			"Quitado"
		));

		mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(loginApi("owner-dashboard-v2@local.invalid", "senha123")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type":"CASA",
					  "name":"Apartamento Central"
					}
					"""))
			.andExpect(status().isCreated());

		criarMembro(owner.householdId(), "Bia", "member-dashboard-v2@local.invalid", "senha123", HouseholdMemberRole.MEMBER);
		String ownerToken = loginApi("owner-dashboard-v2@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/dashboard")
				.header("Authorization", bearer(ownerToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role").value("OWNER"))
			.andExpect(jsonPath("$.data.summaryMain.referenceMonth").exists())
			.andExpect(jsonPath("$.data.summaryMain.openCount").value(2))
			.andExpect(jsonPath("$.data.summaryMain.overdueCount").value(1))
			.andExpect(jsonPath("$.data.summaryMain.totalOpenAmount").value(500.00))
			.andExpect(jsonPath("$.data.summaryMain.totalOverdueAmount").value(120.00))
			.andExpect(jsonPath("$.data.summaryMain.paidThisMonthAmount").value(500.00))
			.andExpect(jsonPath("$.data.actionNeeded.items.length()").value(3))
			.andExpect(jsonPath("$.data.actionNeeded.items[0].description").value("Internet"))
			.andExpect(jsonPath("$.data.actionNeeded.items[0].status").value("VENCIDA"))
			.andExpect(jsonPath("$.data.recentActivity.items.length()").value(5))
			.andExpect(jsonPath("$.data.recentActivity.items[0].type").value("PAYMENT_RECORDED"))
			.andExpect(jsonPath("$.data.assistantCard.route").value("/assistant"))
			.andExpect(jsonPath("$.data.monthOverview.referenceMonth").exists())
			.andExpect(jsonPath("$.data.monthOverview.totalAmount").value(920.00))
			.andExpect(jsonPath("$.data.monthOverview.paidAmount").value(500.00))
			.andExpect(jsonPath("$.data.monthOverview.remainingAmount").value(420.00))
			.andExpect(jsonPath("$.data.monthOverview.highestSpendingCategory.categoryName").value("Moradia"))
			.andExpect(jsonPath("$.data.categorySpending.items.length()").value(1))
			.andExpect(jsonPath("$.data.householdSummary.membersCount").value(2))
			.andExpect(jsonPath("$.data.householdSummary.ownersCount").value(1))
			.andExpect(jsonPath("$.data.householdSummary.membersOnlyCount").value(1))
			.andExpect(jsonPath("$.data.householdSummary.spaceReferencesCount").value(1))
			.andExpect(jsonPath("$.data.householdSummary.referencesByGroup[0].group").value("RESIDENCIAL"))
			.andExpect(jsonPath("$.data.quickActions").doesNotExist());
	}

	@Test
	void deve_retornar_dashboard_reduzido_para_member_sem_blocos_owner_only() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "owner-member-dashboard@local.invalid", "senha123", "Casa Member"));
		Category moradia = categoryRepository.save(owner.householdId(), new Category(null, "Moradia", true));
		Subcategory internet = subcategoryRepository.save(owner.householdId(), new Subcategory(null, moradia.getId(), "Internet", true));
		criarMembro(owner.householdId(), "Bia", "member-dashboard-shape@local.invalid", "senha123", HouseholdMemberRole.MEMBER);

		Expense expense = expenseRepository.save(new Expense(
			owner.householdId(),
			"Internet",
			new BigDecimal("120.00"),
			LocalDate.now().plusDays(2),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			internet.getId(),
			internet.getName(),
			null
		));
		expense.setCreatedAt(Instant.parse("2026-03-29T10:00:00Z"));
		expenseRepository.save(expense);

		String memberToken = loginApi("member-dashboard-shape@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/dashboard")
				.header("Authorization", bearer(memberToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role").value("MEMBER"))
			.andExpect(jsonPath("$.data.summaryMain.openCount").value(1))
			.andExpect(jsonPath("$.data.actionNeeded.items[0].description").value("Internet"))
			.andExpect(jsonPath("$.data.recentActivity.items[0].type").value("EXPENSE_CREATED"))
			.andExpect(jsonPath("$.data.assistantCard.route").value("/assistant"))
			.andExpect(jsonPath("$.data.quickActions.items.length()").value(3))
			.andExpect(jsonPath("$.data.quickActions.items[0].route").value("/expenses"))
			.andExpect(jsonPath("$.data.quickActions.items[1].route").value("/assistant"))
			.andExpect(jsonPath("$.data.quickActions.items[2].route").value("/reports"))
			.andExpect(jsonPath("$.data.monthOverview").doesNotExist())
			.andExpect(jsonPath("$.data.categorySpending").doesNotExist())
			.andExpect(jsonPath("$.data.householdSummary").doesNotExist());
	}

	private String loginApi(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"%s",
					  "password":"%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("accessToken").asText();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private void criarMembro(Long householdId, String name, String email, String password, HouseholdMemberRole role) {
		AppUser user = appUserRepository.save(new AppUser(name, email, passwordEncoder.encode(password)));
		var household = householdRepository.findById(householdId).orElseThrow();
		householdMemberRepository.save(new HouseholdMember(household, user, role));
	}
}
