package com.gilrossi.despesas.identity;

public record AuthResponse(
	Long userId,
	Long householdId,
	String email,
	String name,
	String role
) {
}
