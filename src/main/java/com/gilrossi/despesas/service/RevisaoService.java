package com.gilrossi.despesas.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.emailingestion.EmailIngestionClassification;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionRecord;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewActionResult;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewService;
import com.gilrossi.despesas.model.RevisaoPagina;
import com.gilrossi.despesas.model.RevisaoPendencia;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class RevisaoService {

	private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");
	private static final DateTimeFormatter RECEIVED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE_PT_BR);

	private final EmailIngestionReviewService reviewService;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public RevisaoService(
		EmailIngestionReviewService reviewService,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.reviewService = reviewService;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public RevisaoPagina carregarPagina() {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		List<RevisaoPendencia> pendencias = reviewService.listPending(householdId).stream()
			.map(this::toViewModel)
			.toList();
		return new RevisaoPagina(pendencias);
	}

	@Transactional
	public String aprovar(Long ingestionId) {
		EmailIngestionReviewActionResult result = reviewService.approve(currentHouseholdProvider.requireHouseholdId(), ingestionId);
		return "Ingestão #" + result.ingestionId() + " aprovada e importada na despesa #" + result.importedExpenseId() + ".";
	}

	@Transactional
	public String rejeitar(Long ingestionId) {
		EmailIngestionReviewActionResult result = reviewService.reject(currentHouseholdProvider.requireHouseholdId(), ingestionId);
		return "Ingestão #" + result.ingestionId() + " rejeitada e arquivada.";
	}

	private RevisaoPendencia toViewModel(EmailIngestionRecord record) {
		return new RevisaoPendencia(
			record.id(),
			record.sourceAccount(),
			record.sender(),
			record.subject(),
			record.receivedAt().atZoneSameInstant(ZoneId.systemDefault()).format(RECEIVED_AT_FORMATTER),
			labelForClassification(record.classification()),
			percentFormat().format(record.confidence()),
			StringUtils.hasText(record.merchantOrPayee()) ? record.merchantOrPayee() : "Não identificado",
			record.totalAmount() == null ? "Total ausente" : currencyFormat().format(record.totalAmount()),
			labelForReason(record.decisionReason()),
			StringUtils.hasText(record.summary()) ? record.summary() : "Sem resumo extraído."
		);
	}

	private NumberFormat currencyFormat() {
		return NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
	}

	private NumberFormat percentFormat() {
		NumberFormat format = NumberFormat.getPercentInstance(LOCALE_PT_BR);
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(1);
		return format;
	}

	private String labelForClassification(EmailIngestionClassification classification) {
		return switch (classification) {
			case RECURRING_BILL -> "Cobrança recorrente";
			case MANUAL_PURCHASE -> "Compra manual";
			case FINANCIAL_TRANSACTION -> "Transação financeira";
			case IRRELEVANT -> "Irrelevante";
		};
	}

	private String labelForReason(EmailIngestionDecisionReason reason) {
		return switch (reason) {
			case REVIEW_REQUESTED -> "Confiança insuficiente para autoimportação";
			case CATEGORY_NOT_FOUND -> "Categoria sugerida não mapeada";
			case SUBCATEGORY_NOT_FOUND -> "Subcategoria sugerida não mapeada";
			case SUBCATEGORY_REQUIRED -> "Subcategoria não pôde ser resolvida";
			case UNSUPPORTED_CURRENCY -> "Moeda não suportada";
			case MISSING_TOTAL_AMOUNT -> "Total não extraído";
			case ITEM_TOTAL_MISMATCH -> "Itens não fecham com o total";
			default -> "Revisão manual necessária";
		};
	}
}
