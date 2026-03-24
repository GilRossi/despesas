package com.gilrossi.despesas.identity;

public class AppUserNotFoundException extends RuntimeException {

	public AppUserNotFoundException(String message) {
		super(message);
	}
}
