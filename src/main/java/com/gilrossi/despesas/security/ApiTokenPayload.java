package com.gilrossi.despesas.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ApiTokenPayload(
	String type,
	Long userId,
	Long householdId,
	String role,
	String name,
	String email,
	long exp
) {
}
