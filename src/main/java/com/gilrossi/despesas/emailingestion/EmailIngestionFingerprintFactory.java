package com.gilrossi.despesas.emailingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailIngestionFingerprintFactory {

	public String create(ProcessEmailIngestionCommand command) {
		String sourceAccount = normalize(command.sourceAccount());
		String sender = normalize(command.sender());
		String merchant = normalize(command.merchantOrPayee());
		String subject = normalize(command.subject());
		String currency = normalize(command.currency());
		String amount = command.totalAmount() == null ? "" : command.totalAmount().stripTrailingZeros().toPlainString();
		String effectiveDate = resolveEffectiveDate(command).toString();
		String classification = command.classification() == null ? "" : command.classification().name();
		String payload = String.join(
			"|",
			sourceAccount,
			sender,
			merchant,
			subject,
			amount,
			currency,
			effectiveDate,
			classification
		);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available", exception);
		}
	}

	private LocalDate resolveEffectiveDate(ProcessEmailIngestionCommand command) {
		if (command.occurredOn() != null) {
			return command.occurredOn();
		}
		if (command.dueDate() != null) {
			return command.dueDate();
		}
		return command.receivedAt().toLocalDate();
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
	}
}
