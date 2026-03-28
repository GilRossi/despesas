package com.gilrossi.despesas.spacereference;

public record SpaceReferenceCreateResult(
	SpaceReferenceCreateResultType result,
	SpaceReference reference,
	SpaceReference suggestedReference,
	String message
) {

	public static SpaceReferenceCreateResult created(SpaceReference reference) {
		return new SpaceReferenceCreateResult(SpaceReferenceCreateResultType.CREATED, reference, null, null);
	}

	public static SpaceReferenceCreateResult duplicateSuggestion(SpaceReference suggestedReference, String message) {
		return new SpaceReferenceCreateResult(
			SpaceReferenceCreateResultType.DUPLICATE_SUGGESTION,
			null,
			suggestedReference,
			message
		);
	}

	public boolean isCreated() {
		return result == SpaceReferenceCreateResultType.CREATED;
	}
}
