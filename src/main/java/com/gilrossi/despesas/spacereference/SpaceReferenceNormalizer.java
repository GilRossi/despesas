package com.gilrossi.despesas.spacereference;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.util.StringUtils;

public final class SpaceReferenceNormalizer {

	private SpaceReferenceNormalizer() {
	}

	public static String sanitizeDisplayName(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim().replaceAll("\\s+", " ");
	}

	public static String normalizeName(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "");
		return normalized.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

	public static String normalizeQuery(String value) {
		String normalized = normalizeName(value);
		return StringUtils.hasText(normalized) ? normalized : null;
	}
}
