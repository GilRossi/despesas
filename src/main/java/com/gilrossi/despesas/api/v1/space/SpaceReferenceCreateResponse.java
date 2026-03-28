package com.gilrossi.despesas.api.v1.space;

import com.gilrossi.despesas.spacereference.SpaceReferenceCreateResult;
import com.gilrossi.despesas.spacereference.SpaceReferenceCreateResultType;

public record SpaceReferenceCreateResponse(
	SpaceReferenceCreateResultType result,
	SpaceReferenceResponse reference,
	SpaceReferenceResponse suggestedReference,
	String message
) {

	public static SpaceReferenceCreateResponse from(SpaceReferenceCreateResult result) {
		return new SpaceReferenceCreateResponse(
			result.result(),
			result.reference() == null ? null : SpaceReferenceResponse.from(result.reference()),
			result.suggestedReference() == null ? null : SpaceReferenceResponse.from(result.suggestedReference()),
			result.message()
		);
	}
}
