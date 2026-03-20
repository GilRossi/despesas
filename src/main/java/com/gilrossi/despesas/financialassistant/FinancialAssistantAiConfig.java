package com.gilrossi.despesas.financialassistant;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gilrossi.despesas.financialassistant.ai.DeepSeekFinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantAiService;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.LangChain4jFinancialAssistantTools;
import com.gilrossi.despesas.financialassistant.ai.UnavailableFinancialAssistantConversationGateway;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

@Configuration
@EnableConfigurationProperties(FinancialAssistantAiProperties.class)
public class FinancialAssistantAiConfig {

	@Bean
	FinancialAssistantConversationGateway financialAssistantConversationGateway(
		FinancialAssistantAiProperties properties,
		LangChain4jFinancialAssistantTools tools
	) {
		if (!properties.isEnabled() || !properties.isConfigured()) {
			return new UnavailableFinancialAssistantConversationGateway();
		}

		OpenAiChatModel chatModel = OpenAiChatModel.builder()
			.baseUrl(properties.getBaseUrl())
			.apiKey(properties.getApiKey())
			.modelName(properties.getModel())
			.temperature(properties.getTemperature())
			.maxCompletionTokens(properties.getMaxCompletionTokens())
			.timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
			.maxRetries(properties.getMaxRetries())
			.logRequests(properties.isLogRequests())
			.logResponses(properties.isLogResponses())
			.build();

		FinancialAssistantAiService aiService = AiServices.builder(FinancialAssistantAiService.class)
			.chatModel(chatModel)
			.tools(tools)
			.build();

		return new DeepSeekFinancialAssistantConversationGateway(aiService);
	}
}
