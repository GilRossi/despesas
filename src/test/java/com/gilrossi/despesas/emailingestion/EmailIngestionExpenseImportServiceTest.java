package com.gilrossi.despesas.emailingestion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseService;

@ExtendWith(MockitoExtension.class)
class EmailIngestionExpenseImportServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SubcategoryRepository subcategoryRepository;

	@Mock
	private ExpenseService expenseService;

	private EmailIngestionExpenseImportService service;

	@BeforeEach
	void setUp() {
		service = new EmailIngestionExpenseImportService(categoryRepository, subcategoryRepository, expenseService);
	}

	@Test
	void deve_importar_despesa_quando_categoria_e_subcategoria_estao_resolvidas() {
		when(categoryRepository.findByNameIgnoreCase(7L, "Pets")).thenReturn(Optional.of(new Category(20L, "Pets", true)));
		when(subcategoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(new Subcategory(30L, 20L, "Pet shop", true)));
		when(expenseService.criarParaHousehold(eq(7L), any())).thenReturn(new ExpenseResponse(
			100L,
			"Cobasi",
			new BigDecimal("289.70"),
			LocalDate.of(2026, 3, 19),
			LocalDate.of(2026, 3, 19),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			0,
			false,
			null,
			null
		));

		service.importExpense(7L, "financeiro@gmail.com", new ProcessEmailIngestionCommand(
			"financeiro@gmail.com",
			"msg-1",
			"noreply@cobasi.com.br",
			"Compra Cobasi",
			OffsetDateTime.parse("2026-03-19T10:15:30Z"),
			"Cobasi",
			"Pets",
			"Pet shop",
			new BigDecimal("289.70"),
			null,
			LocalDate.of(2026, 3, 19),
			"BRL",
			List.of(new EmailIngestionItem("Ração", new BigDecimal("199.90"), null)),
			"Compra pet shop",
			EmailIngestionClassification.MANUAL_PURCHASE,
			new BigDecimal("0.98"),
			"gmail:msg-1",
			EmailIngestionDesiredDecision.AUTO_IMPORT
		));

		ArgumentCaptor<CreateExpenseRequest> requestCaptor = ArgumentCaptor.forClass(CreateExpenseRequest.class);
		verify(expenseService).criarParaHousehold(eq(7L), requestCaptor.capture());
		CreateExpenseRequest request = requestCaptor.getValue();
		org.assertj.core.api.Assertions.assertThat(request.categoryId()).isEqualTo(20L);
		org.assertj.core.api.Assertions.assertThat(request.subcategoryId()).isEqualTo(30L);
		org.assertj.core.api.Assertions.assertThat(request.description()).isEqualTo("Cobasi");
	}

	@Test
	void deve_exigir_subcategoria_resolvivel_quando_categoria_tem_multiplas_opcoes() {
		when(categoryRepository.findByNameIgnoreCase(7L, "Moradia")).thenReturn(Optional.of(new Category(20L, "Moradia", true)));
		when(subcategoryRepository.findActiveByHouseholdId(7L)).thenReturn(List.of(
			new Subcategory(30L, 20L, "Internet", true),
			new Subcategory(31L, 20L, "Energia", true)
		));

		assertThatThrownBy(() -> service.importExpense(7L, "financeiro@gmail.com", new ProcessEmailIngestionCommand(
			"financeiro@gmail.com",
			"msg-1",
			"conta@provedor.com",
			"Conta março",
			OffsetDateTime.parse("2026-03-19T10:15:30Z"),
			"Provedor",
			"Moradia",
			null,
			new BigDecimal("120.00"),
			LocalDate.of(2026, 3, 25),
			null,
			"BRL",
			List.of(),
			"Internet março",
			EmailIngestionClassification.RECURRING_BILL,
			new BigDecimal("0.98"),
			"gmail:msg-1",
			EmailIngestionDesiredDecision.AUTO_IMPORT
		)))
			.isInstanceOf(EmailIngestionImportReviewException.class)
			.hasMessage("Auto import requires a resolvable subcategory");
	}
}
