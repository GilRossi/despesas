package com.gilrossi.despesas.financialassistant.ai;

import com.gilrossi.despesas.financialassistant.FinancialAssistantAiUsage;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.service.Result;

public class DeepSeekFinancialAssistantConversationGateway implements FinancialAssistantConversationGateway {

	private final FinancialAssistantAiService aiService;

	public DeepSeekFinancialAssistantConversationGateway(FinancialAssistantAiService aiService) {
		this.aiService = aiService;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public FinancialAssistantConversationResult answer(FinancialAssistantConversationRequest request) {
		try {
			Result<String> result = aiService.answer(
				request.question(),
				request.referenceMonth(),
				request.resolvedIntent(),
				request.resolvedCategoryName()
			);
			return new FinancialAssistantConversationResult(result.content(), extractUsage(result));
		} catch (RuntimeException exception) {
			throw FinancialAssistantGatewayException.from(exception);
		}
	}

	private FinancialAssistantAiUsage extractUsage(Result<String> result) {
		ChatResponse finalResponse = result.finalResponse();
		if (finalResponse == null) {
			return null;
		}

		ChatResponseMetadata metadata = finalResponse.metadata();
		OpenAiTokenUsage openAiTokenUsage = metadata instanceof OpenAiChatResponseMetadata openAiMetadata ? openAiMetadata.tokenUsage() : null;
		Integer inputTokens = finalResponse.tokenUsage() == null ? null : finalResponse.tokenUsage().inputTokenCount();
		Integer outputTokens = finalResponse.tokenUsage() == null ? null : finalResponse.tokenUsage().outputTokenCount();
		Integer totalTokens = finalResponse.tokenUsage() == null ? null : finalResponse.tokenUsage().totalTokenCount();
		Integer promptCacheHitTokens = openAiTokenUsage == null || openAiTokenUsage.inputTokensDetails() == null
			? null
			: openAiTokenUsage.inputTokensDetails().cachedTokens();
		Integer promptCacheMissTokens = inputTokens == null || promptCacheHitTokens == null
			? inputTokens
			: Math.max(0, inputTokens - promptCacheHitTokens);

		return new FinancialAssistantAiUsage(
			finalResponse.modelName(),
			inputTokens,
			outputTokens,
			totalTokens,
			promptCacheHitTokens,
			promptCacheMissTokens,
			result.toolExecutions() == null ? 0 : result.toolExecutions().size(),
			result.finishReason() == null ? null : result.finishReason().name()
		);
	}
}
