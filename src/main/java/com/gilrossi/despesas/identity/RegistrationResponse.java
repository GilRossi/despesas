package com.gilrossi.despesas.identity;

public record RegistrationResponse(Long userId, Long householdId, String name, String email, String role) {

	public static RegistrationResponse from(AppUser user, HouseholdMember member) {
		return new RegistrationResponse(
			user.getId(),
			member.getHouseholdId(),
			user.getName(),
			user.getEmail(),
			member.getRole().name()
		);
	}
}
