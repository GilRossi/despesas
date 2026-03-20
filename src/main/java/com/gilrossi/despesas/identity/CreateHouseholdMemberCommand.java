package com.gilrossi.despesas.identity;

public record CreateHouseholdMemberCommand(
	String name,
	String email,
	String password,
	HouseholdMemberRole role
) {
}
