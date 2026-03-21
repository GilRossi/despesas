package com.gilrossi.despesas.emailingestion;

public class DuplicateEmailIngestionSourceException extends RuntimeException {

	public DuplicateEmailIngestionSourceException(String sourceAccount) {
		super("Source account is already mapped: " + sourceAccount);
	}
}
