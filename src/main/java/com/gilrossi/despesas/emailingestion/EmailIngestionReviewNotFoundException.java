package com.gilrossi.despesas.emailingestion;

public class EmailIngestionReviewNotFoundException extends RuntimeException {

	public EmailIngestionReviewNotFoundException(Long ingestionId) {
		super("Ingestão #" + ingestionId + " não foi encontrada para o household atual.");
	}
}
