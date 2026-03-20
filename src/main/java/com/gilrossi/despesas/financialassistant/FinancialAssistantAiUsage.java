package com.gilrossi.despesas.financialassistant;

public record FinancialAssistantAiUsage(
	String modelName,
	Integer inputTokens,
	Integer outputTokens,
	Integer totalTokens,
	Integer promptCacheHitTokens,
	Integer promptCacheMissTokens,
	Integer toolExecutions,
	String finishReason
) {

	public FinancialAssistantAiUsage {
		validateNonNegative("inputTokens", inputTokens);
		validateNonNegative("outputTokens", outputTokens);
		validateNonNegative("totalTokens", totalTokens);
		validateNonNegative("promptCacheHitTokens", promptCacheHitTokens);
		validateNonNegative("promptCacheMissTokens", promptCacheMissTokens);
		validateNonNegative("toolExecutions", toolExecutions);

		if (inputTokens != null && outputTokens != null && totalTokens != null && totalTokens.intValue() != inputTokens + outputTokens) {
			throw new IllegalArgumentException("totalTokens must be equal to inputTokens + outputTokens");
		}
		if (inputTokens != null
			&& promptCacheHitTokens != null
			&& promptCacheMissTokens != null
			&& inputTokens.intValue() != promptCacheHitTokens + promptCacheMissTokens) {
			throw new IllegalArgumentException("inputTokens must be equal to promptCacheHitTokens + promptCacheMissTokens");
		}
	}

	private static void validateNonNegative(String fieldName, Integer value) {
		if (value != null && value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
	}
}
