package com.gilrossi.despesas.identity;

public class DuplicateRegistrationException extends RuntimeException {

	public DuplicateRegistrationException(String email) {
		super("User with email '" + email + "' already exists");
	}
}
