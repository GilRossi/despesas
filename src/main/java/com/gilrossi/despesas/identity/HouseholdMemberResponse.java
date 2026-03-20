package com.gilrossi.despesas.identity;

public record HouseholdMemberResponse(
	Long id,
	Long userId,
	Long householdId,
	String name,
	String email,
	HouseholdMemberRole role
) {

	public static HouseholdMemberResponse from(HouseholdMember member) {
		return new HouseholdMemberResponse(
			member.getId(),
			member.getUserId(),
			member.getHouseholdId(),
			member.getUser().getName(),
			member.getUser().getEmail(),
			member.getRole()
		);
	}
}
