package com.gilrossi.despesas.spacereference;

public record CreateSpaceReferenceCommand(
	SpaceReferenceType type,
	String name
) {
}
