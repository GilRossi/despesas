package com.gilrossi.despesas.model;

public record RevisaoPendencia(
	Long id,
	String sourceAccount,
	String sender,
	String subject,
	String receivedAtLabel,
	String classificationLabel,
	String confidenceLabel,
	String merchantOrPayee,
	String totalAmountLabel,
	String reasonLabel,
	String summary
) {
}
