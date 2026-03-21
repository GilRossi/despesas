package com.gilrossi.despesas.emailingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseResponse;

@ExtendWith(MockitoExtension.class)
class EmailIngestionReviewServiceTest {

	@Mock
	private EmailIngestionRecordRepository recordRepository;

	@Mock
	private EmailIngestionExpenseImportService expenseImportService;

	private EmailIngestionReviewService service;

	@BeforeEach
	void setUp() {
		service = new EmailIngestionReviewService(recordRepository, expenseImportService);
	}

	@Test
	void deve_listar_pendencias_do_household() {
		when(recordRepository.findAllPendingReviewByHouseholdId(9L)).thenReturn(List.of(reviewRecord(51L, EmailIngestionDecisionReason.REVIEW_REQUESTED)));

		List<EmailIngestionRecord> pendencias = service.listPending(9L);

		assertThat(pendencias).hasSize(1);
		assertThat(pendencias.getFirst().id()).isEqualTo(51L);
	}

	@Test
	void deve_aprovar_pendencia_e_importar_despesa() {
		EmailIngestionRecord record = reviewRecord(51L, EmailIngestionDecisionReason.REVIEW_REQUESTED);
		when(recordRepository.findByIdAndHouseholdIdForUpdate(51L, 9L)).thenReturn(Optional.of(record));
		when(expenseImportService.importExpense(eq(9L), eq("financeiro@gmail.com"), any())).thenReturn(new ExpenseResponse(
			88L,
			"Cobasi",
			new BigDecimal("289.70"),
			LocalDate.of(2026, 3, 19),
			ExpenseContext.PETS,
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
		when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		EmailIngestionReviewActionResult result = service.approve(9L, 51L);

		assertThat(result.finalDecision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
		assertThat(result.decisionReason()).isEqualTo(EmailIngestionDecisionReason.MANUALLY_IMPORTED);
		assertThat(result.importedExpenseId()).isEqualTo(88L);
		verify(expenseImportService).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_rejeitar_pendencia_sem_criar_despesa() {
		EmailIngestionRecord record = reviewRecord(52L, EmailIngestionDecisionReason.ITEM_TOTAL_MISMATCH);
		when(recordRepository.findByIdAndHouseholdIdForUpdate(52L, 9L)).thenReturn(Optional.of(record));
		when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		EmailIngestionReviewActionResult result = service.reject(9L, 52L);

		assertThat(result.finalDecision()).isEqualTo(EmailIngestionFinalDecision.IGNORED);
		assertThat(result.decisionReason()).isEqualTo(EmailIngestionDecisionReason.MANUALLY_REJECTED);
		assertThat(result.importedExpenseId()).isNull();
		verify(expenseImportService, never()).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_impedir_aprovacao_quando_total_estiver_ausente() {
		EmailIngestionRecord record = new EmailIngestionRecord(
			53L,
			9L,
			3L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"msg-53",
			"noreply@empresa.com",
			"Compra manual",
			OffsetDateTime.parse("2026-03-19T10:15:30Z"),
			"Cobasi",
			"Pets",
			"Pet shop",
			null,
			null,
			LocalDate.of(2026, 3, 19),
			"BRL",
			"Compra manual",
			EmailIngestionClassification.MANUAL_PURCHASE,
			new BigDecimal("0.70"),
			"gmail:msg-53",
			EmailIngestionDesiredDecision.AUTO_IMPORT,
			EmailIngestionFinalDecision.REVIEW_REQUIRED,
			EmailIngestionDecisionReason.MISSING_TOTAL_AMOUNT,
			"fp-53",
			null,
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			List.of()
		);
		when(recordRepository.findByIdAndHouseholdIdForUpdate(53L, 9L)).thenReturn(Optional.of(record));

		assertThatThrownBy(() -> service.approve(9L, 53L))
			.isInstanceOf(EmailIngestionReviewActionNotAllowedException.class)
			.hasMessage("Esta ingestão ainda não pode ser aprovada porque o total não foi extraído.");

		verify(expenseImportService, never()).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_impedir_tratamento_de_ingestao_ja_importada() {
		EmailIngestionRecord record = new EmailIngestionRecord(
			54L,
			9L,
			3L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"msg-54",
			"conta@claro.com.br",
			"Claro Internet março",
			OffsetDateTime.parse("2026-03-19T10:15:30Z"),
			"Claro Internet",
			"Casa",
			"Internet",
			new BigDecimal("120.00"),
			LocalDate.of(2026, 3, 25),
			null,
			"BRL",
			"Cobrança recorrente mensal",
			EmailIngestionClassification.RECURRING_BILL,
			new BigDecimal("0.96"),
			"gmail:msg-54",
			EmailIngestionDesiredDecision.AUTO_IMPORT,
			EmailIngestionFinalDecision.AUTO_IMPORTED,
			EmailIngestionDecisionReason.IMPORTED,
			"fp-54",
			81L,
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			List.of()
		);
		when(recordRepository.findByIdAndHouseholdIdForUpdate(54L, 9L)).thenReturn(Optional.of(record));

		assertThatThrownBy(() -> service.reject(9L, 54L))
			.isInstanceOf(EmailIngestionReviewActionNotAllowedException.class)
			.hasMessage("A ingestão selecionada não está mais pendente de revisão.");
	}

	private EmailIngestionRecord reviewRecord(Long id, EmailIngestionDecisionReason reason) {
		return new EmailIngestionRecord(
			id,
			9L,
			3L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"msg-" + id,
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
			"Compra pet shop",
			EmailIngestionClassification.MANUAL_PURCHASE,
			new BigDecimal("0.72"),
			"gmail:msg-" + id,
			EmailIngestionDesiredDecision.AUTO_IMPORT,
			EmailIngestionFinalDecision.REVIEW_REQUIRED,
			reason,
			"fp-" + id,
			null,
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			List.of(new EmailIngestionItem("Ração", new BigDecimal("289.70"), null))
		);
	}
}
