package com.gilrossi.despesas.api.v1.space;

import com.gilrossi.despesas.spacereference.SpaceReference;
import com.gilrossi.despesas.spacereference.SpaceReferenceType;
import com.gilrossi.despesas.spacereference.SpaceReferenceTypeGroup;

public record SpaceReferenceResponse(
	Long id,
	SpaceReferenceType type,
	SpaceReferenceTypeGroup typeGroup,
	String name
) {

	public static SpaceReferenceResponse from(SpaceReference reference) {
		return new SpaceReferenceResponse(
			reference.getId(),
			reference.getType(),
			reference.getTypeGroup(),
			reference.getName()
		);
	}
}
