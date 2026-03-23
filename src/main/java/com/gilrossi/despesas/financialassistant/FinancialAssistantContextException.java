package com.gilrossi.despesas.financialassistant;

public class FinancialAssistantContextException extends IllegalStateException {

	private final String reasonCode;
	private final Long userId;
	private final Long householdId;
	private final String role;

	private FinancialAssistantContextException(
		String reasonCode,
		String message,
		Long userId,
		Long householdId,
		String role
	) {
		super(message);
		this.reasonCode = reasonCode;
		this.userId = userId;
		this.householdId = householdId;
		this.role = role;
	}

	public static FinancialAssistantContextException invalidHouseholdScope(Long userId, String role) {
		return new FinancialAssistantContextException(
			"ASSISTANT_INVALID_HOUSEHOLD_CONTEXT",
			"Active household membership is required for financial assistant queries",
			userId,
			null,
			role
		);
	}

	public String reasonCode() {
		return reasonCode;
	}

	public Long userId() {
		return userId;
	}

	public Long householdId() {
		return householdId;
	}

	public String role() {
		return role;
	}
}
