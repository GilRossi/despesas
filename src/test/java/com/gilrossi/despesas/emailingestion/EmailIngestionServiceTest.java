package com.gilrossi.despesas.emailingestion;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionOperations;

import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.security.OperationalEmailIngestionAuditLogger;

@ExtendWith(MockitoExtension.class)
class EmailIngestionServiceTest {

	@Mock
	private EmailIngestionSourceRepository sourceRepository;

	@Mock
	private EmailIngestionRecordRepository recordRepository;

	@Mock
	private EmailIngestionFingerprintFactory fingerprintFactory;

	@Mock
	private EmailIngestionExpenseImportService expenseImportService;

	private EmailIngestionService service;

	@BeforeEach
	void setUp() {
		service = new EmailIngestionService(
			sourceRepository,
			recordRepository,
			fingerprintFactory,
			expenseImportService,
			TransactionOperations.withoutTransaction(),
			new OperationalEmailIngestionAuditLogger()
		);
	}

	@Test
	void deve_auto_importar_quando_confianca_for_alta_e_payload_estiver_pronto() {
		EmailIngestionSource source = source();
		ProcessEmailIngestionCommand command = command(new BigDecimal("0.96"), EmailIngestionClassification.MANUAL_PURCHASE, EmailIngestionDesiredDecision.AUTO_IMPORT);
		when(sourceRepository.findActiveByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(source));
		when(recordRepository.findBySourceAccountAndExternalMessageId("financeiro@gmail.com", "msg-1")).thenReturn(Optional.empty());
		when(recordRepository.findLatestByHouseholdIdAndFingerprint(9L, "fp-1")).thenReturn(Optional.empty());
		when(fingerprintFactory.create(any())).thenReturn("fp-1");
		when(expenseImportService.importExpense(eq(9L), eq("financeiro@gmail.com"), any())).thenReturn(new ExpenseResponse(
			88L,
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
		when(recordRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 15L));

		EmailIngestionResult result = service.process(command);

		assertThat(result.decision()).isEqualTo(EmailIngestionFinalDecision.AUTO_IMPORTED);
		assertThat(result.reason()).isEqualTo(EmailIngestionDecisionReason.IMPORTED);
		assertThat(result.expenseId()).isEqualTo(88L);
		verify(expenseImportService).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_enviar_para_review_quando_confianca_for_media() {
		EmailIngestionSource source = source();
		ProcessEmailIngestionCommand command = command(new BigDecimal("0.70"), EmailIngestionClassification.RECURRING_BILL, EmailIngestionDesiredDecision.AUTO_IMPORT);
		when(sourceRepository.findActiveByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(source));
		when(recordRepository.findBySourceAccountAndExternalMessageId("financeiro@gmail.com", "msg-1")).thenReturn(Optional.empty());
		when(recordRepository.findLatestByHouseholdIdAndFingerprint(9L, "fp-1")).thenReturn(Optional.empty());
		when(fingerprintFactory.create(any())).thenReturn("fp-1");
		when(recordRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 16L));

		EmailIngestionResult result = service.process(command);

		assertThat(result.decision()).isEqualTo(EmailIngestionFinalDecision.REVIEW_REQUIRED);
		assertThat(result.reason()).isEqualTo(EmailIngestionDecisionReason.REVIEW_REQUESTED);
		verify(expenseImportService, never()).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_ignorar_quando_classificacao_for_irrelevante() {
		EmailIngestionSource source = source();
		ProcessEmailIngestionCommand command = command(new BigDecimal("0.92"), EmailIngestionClassification.IRRELEVANT, EmailIngestionDesiredDecision.AUTO_IMPORT);
		when(sourceRepository.findActiveByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(source));
		when(recordRepository.findBySourceAccountAndExternalMessageId("financeiro@gmail.com", "msg-1")).thenReturn(Optional.empty());
		when(recordRepository.findLatestByHouseholdIdAndFingerprint(9L, "fp-1")).thenReturn(Optional.empty());
		when(fingerprintFactory.create(any())).thenReturn("fp-1");
		when(recordRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 17L));

		EmailIngestionResult result = service.process(command);

		assertThat(result.decision()).isEqualTo(EmailIngestionFinalDecision.IGNORED);
		assertThat(result.reason()).isEqualTo(EmailIngestionDecisionReason.IRRELEVANT_CLASSIFICATION);
		verify(expenseImportService, never()).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_ignorar_duplicado_sem_persistir_novo_registro() {
		EmailIngestionSource source = source();
		ProcessEmailIngestionCommand command = command(new BigDecimal("0.96"), EmailIngestionClassification.MANUAL_PURCHASE, EmailIngestionDesiredDecision.AUTO_IMPORT);
		when(sourceRepository.findActiveByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(source));
		when(recordRepository.findBySourceAccountAndExternalMessageId("financeiro@gmail.com", "msg-1")).thenReturn(Optional.of(new EmailIngestionRecord(
			50L,
			9L,
			3L,
			"financeiro@gmail.com",
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
			"Compra pet shop",
			EmailIngestionClassification.MANUAL_PURCHASE,
			new BigDecimal("0.96"),
			"gmail:msg-1",
			EmailIngestionDesiredDecision.AUTO_IMPORT,
			EmailIngestionFinalDecision.AUTO_IMPORTED,
			EmailIngestionDecisionReason.IMPORTED,
			"fp-1",
			88L,
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			List.of()
		)));

		EmailIngestionResult result = service.process(command);

		assertThat(result.decision()).isEqualTo(EmailIngestionFinalDecision.IGNORED);
		assertThat(result.reason()).isEqualTo(EmailIngestionDecisionReason.DUPLICATE_MESSAGE_ID);
		assertThat(result.duplicate()).isTrue();
		verify(recordRepository, never()).save(any());
		verify(expenseImportService, never()).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_enviar_para_review_quando_total_dos_itens_nao_bate() {
		EmailIngestionSource source = source();
		ProcessEmailIngestionCommand command = new ProcessEmailIngestionCommand(
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
			List.of(
				new EmailIngestionItem("Ração", new BigDecimal("100.00"), null),
				new EmailIngestionItem("Areia", new BigDecimal("100.00"), null)
			),
			"Compra pet shop",
			EmailIngestionClassification.MANUAL_PURCHASE,
			new BigDecimal("0.96"),
			"gmail:msg-1",
			EmailIngestionDesiredDecision.AUTO_IMPORT
		);
		when(sourceRepository.findActiveByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(source));
		when(recordRepository.findBySourceAccountAndExternalMessageId("financeiro@gmail.com", "msg-1")).thenReturn(Optional.empty());
		when(recordRepository.findLatestByHouseholdIdAndFingerprint(9L, "fp-1")).thenReturn(Optional.empty());
		when(fingerprintFactory.create(any())).thenReturn("fp-1");
		when(recordRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 18L));

		EmailIngestionResult result = service.process(command);

		assertThat(result.decision()).isEqualTo(EmailIngestionFinalDecision.REVIEW_REQUIRED);
		assertThat(result.reason()).isEqualTo(EmailIngestionDecisionReason.ITEM_TOTAL_MISMATCH);
		verify(expenseImportService, never()).importExpense(eq(9L), eq("financeiro@gmail.com"), any());
	}

	@Test
	void deve_degradar_para_duplicado_quando_houver_conflito_concorrente_de_message_id() {
		EmailIngestionSource source = source();
		ProcessEmailIngestionCommand command = command(new BigDecimal("0.96"), EmailIngestionClassification.RECURRING_BILL, EmailIngestionDesiredDecision.AUTO_IMPORT);
		EmailIngestionRecord existing = new EmailIngestionRecord(
			60L,
			9L,
			3L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"msg-1",
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
			"gmail:msg-1",
			EmailIngestionDesiredDecision.AUTO_IMPORT,
			EmailIngestionFinalDecision.AUTO_IMPORTED,
			EmailIngestionDecisionReason.IMPORTED,
			"fp-1",
			88L,
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			List.of()
		);
		when(sourceRepository.findActiveByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(source));
		when(recordRepository.findBySourceAccountAndExternalMessageId("financeiro@gmail.com", "msg-1"))
			.thenReturn(Optional.empty(), Optional.of(existing));
		when(recordRepository.findLatestByHouseholdIdAndFingerprint(9L, "fp-1")).thenReturn(Optional.empty());
		when(fingerprintFactory.create(any())).thenReturn("fp-1");
		when(expenseImportService.importExpense(eq(9L), eq("financeiro@gmail.com"), any())).thenReturn(new ExpenseResponse(
			88L,
			"Claro Internet",
			new BigDecimal("120.00"),
			LocalDate.of(2026, 3, 25),
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
		when(recordRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate message"));

		EmailIngestionResult result = service.process(command);

		assertThat(result.decision()).isEqualTo(EmailIngestionFinalDecision.IGNORED);
		assertThat(result.reason()).isEqualTo(EmailIngestionDecisionReason.DUPLICATE_MESSAGE_ID);
		assertThat(result.duplicate()).isTrue();
	}

	private EmailIngestionSource source() {
		return new EmailIngestionSource(
			3L,
			9L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			"Gmail pessoal",
			true,
			new BigDecimal("0.90"),
			new BigDecimal("0.65"),
			OffsetDateTime.now(),
			OffsetDateTime.now()
		);
	}

	private ProcessEmailIngestionCommand command(
		BigDecimal confidence,
		EmailIngestionClassification classification,
		EmailIngestionDesiredDecision desiredDecision
	) {
		return new ProcessEmailIngestionCommand(
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
			List.of(new EmailIngestionItem("Ração", new BigDecimal("289.70"), null)),
			"Compra pet shop",
			classification,
			confidence,
			"gmail:msg-1",
			desiredDecision
		);
	}

	private EmailIngestionRecord withId(EmailIngestionRecord record, Long id) {
		return new EmailIngestionRecord(
			id,
			record.householdId(),
			record.sourceId(),
			record.sourceAccount(),
			record.normalizedSourceAccount(),
			record.externalMessageId(),
			record.sender(),
			record.subject(),
			record.receivedAt(),
			record.merchantOrPayee(),
			record.suggestedCategoryName(),
			record.suggestedSubcategoryName(),
			record.totalAmount(),
			record.dueDate(),
			record.occurredOn(),
			record.currency(),
			record.summary(),
			record.classification(),
			record.confidence(),
			record.rawReference(),
			record.desiredDecision(),
			record.finalDecision(),
			record.decisionReason(),
			record.fingerprint(),
			record.importedExpenseId(),
			OffsetDateTime.now(),
			OffsetDateTime.now(),
			record.items()
		);
	}
}
