package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class FinancialAssistantSupport {

	private FinancialAssistantSupport() {
	}

	public static FinancialAssistantDateRange resolveRange(LocalDate from, LocalDate to) {
		if (from == null && to == null) {
			return monthRange(YearMonth.now());
		}
		LocalDate resolvedFrom = from;
		LocalDate resolvedTo = to;
		if (resolvedFrom == null) {
			YearMonth month = YearMonth.from(resolvedTo);
			resolvedFrom = month.atDay(1);
		}
		if (resolvedTo == null) {
			YearMonth month = YearMonth.from(resolvedFrom);
			resolvedTo = month.atEndOfMonth();
		}
		if (resolvedFrom.isAfter(resolvedTo)) {
			throw new IllegalArgumentException("from must be before or equal to to");
		}
		return new FinancialAssistantDateRange(resolvedFrom, resolvedTo);
	}

	public static FinancialAssistantDateRange monthRange(YearMonth month) {
		return new FinancialAssistantDateRange(month.atDay(1), month.atEndOfMonth());
	}

	public static YearMonth resolveReferenceMonth(String referenceMonth) {
		if (referenceMonth == null || referenceMonth.isBlank()) {
			return YearMonth.now();
		}
		try {
			return YearMonth.parse(referenceMonth.trim());
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("referenceMonth must use yyyy-MM");
		}
	}

	public static String normalizeText(String value) {
		if (value == null) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "");
		return normalized.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

	public static BigDecimal percentage(BigDecimal value, BigDecimal total) {
		if (value == null || total == null || total.signum() == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return value
			.multiply(new BigDecimal("100"))
			.divide(total, 2, RoundingMode.HALF_UP);
	}

	public static BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
		if (current == null) {
			current = BigDecimal.ZERO;
		}
		if (previous == null || previous.signum() == 0) {
			return current.signum() == 0
				? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: new BigDecimal("100.00");
		}
		return current.subtract(previous)
			.multiply(new BigDecimal("100"))
			.divide(previous, 2, RoundingMode.HALF_UP);
	}

	public static String monthLabel(YearMonth month) {
		return month.toString();
	}
}
