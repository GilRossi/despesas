package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.financialassistant.ai.DeepSeekFinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationRequest;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationResult;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantAiService;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantGatewayException;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.Result;

@ExtendWith(MockitoExtension.class)
class DeepSeekFinancialAssistantConversationGatewayTest {

	@Mock
	private FinancialAssistantAiService aiService;

	@Mock
	private Result<String> result;

	@Mock
	private ChatResponse chatResponse;

	@Mock
	private OpenAiChatResponseMetadata metadata;

	@Mock
	private OpenAiTokenUsage tokenUsage;

	@Mock
	private OpenAiTokenUsage.InputTokensDetails inputTokensDetails;

	@Test
	@SuppressWarnings("unchecked")
	void deve_delegar_resposta_para_o_ai_service_sem_chamada_externa_real() {
		when(aiService.answer(anyString(), anyString(), anyString(), nullable(String.class))).thenReturn(result);
		when(result.content()).thenReturn("Revise a categoria com maior peso.");
		when(result.finalResponse()).thenReturn(chatResponse);
		when(result.finishReason()).thenReturn(FinishReason.STOP);
		when(result.toolExecutions()).thenReturn(List.of());
		when(chatResponse.metadata()).thenReturn(metadata);
		when(chatResponse.modelName()).thenReturn("deepseek-chat");
		when(chatResponse.tokenUsage()).thenReturn(tokenUsage);
		when(metadata.tokenUsage()).thenReturn(tokenUsage);
		when(tokenUsage.inputTokenCount()).thenReturn(120);
		when(tokenUsage.outputTokenCount()).thenReturn(45);
		when(tokenUsage.totalTokenCount()).thenReturn(165);
		when(tokenUsage.inputTokensDetails()).thenReturn(inputTokensDetails);
		when(inputTokensDetails.cachedTokens()).thenReturn(100);
		DeepSeekFinancialAssistantConversationGateway gateway = new DeepSeekFinancialAssistantConversationGateway(aiService);
		FinancialAssistantConversationResult result = gateway.answer(new FinancialAssistantConversationRequest(
			"Como posso economizar este mês?",
			"2026-03",
			"SAVINGS_RECOMMENDATIONS",
			null
		));

		assertTrue(gateway.isAvailable());
		assertEquals("Revise a categoria com maior peso.", result.answer());
		assertNotNull(result.usage());
		assertEquals("deepseek-chat", result.usage().modelName());
		assertEquals(120, result.usage().inputTokens());
		assertEquals(45, result.usage().outputTokens());
		assertEquals(165, result.usage().totalTokens());
		assertEquals(100, result.usage().promptCacheHitTokens());
		assertEquals(20, result.usage().promptCacheMissTokens());
	}

	@Test
	void deve_encapsular_falha_do_provedor_em_gateway_exception_com_categoria_estavel() {
		when(aiService.answer(anyString(), anyString(), anyString(), nullable(String.class)))
			.thenThrow(new ProviderAuthenticationException("{\"error\":{\"type\":\"authentication_error\"}}"));
		DeepSeekFinancialAssistantConversationGateway gateway = new DeepSeekFinancialAssistantConversationGateway(aiService);

		FinancialAssistantGatewayException exception = assertThrows(
			FinancialAssistantGatewayException.class,
			() -> gateway.answer(new FinancialAssistantConversationRequest(
				"Como posso economizar este mês?",
				"2026-03",
				"SAVINGS_RECOMMENDATIONS",
				""
			))
		);

		assertEquals(FinancialAssistantAiFailureCategory.AUTH_ERROR, exception.category());
		assertEquals("Financial assistant provider call failed", exception.getMessage());
	}

	private static class ProviderAuthenticationException extends RuntimeException {
		ProviderAuthenticationException(String message) {
			super(message);
		}
	}
}
