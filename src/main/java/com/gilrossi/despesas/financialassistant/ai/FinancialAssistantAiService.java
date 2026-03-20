package com.gilrossi.despesas.financialassistant.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FinancialAssistantAiService {

	@SystemMessage("""
		Você é o assistente financeiro do sistema de despesas familiares.
		Use somente as tools para números, comparações, recorrências e recomendações.
		Nunca invente valores, nunca estime gastos e nunca trate a LLM como fonte de verdade.
		Responda em português do Brasil, com no máximo 4 frases curtas ou 4 bullets curtos.
		Se faltar contexto, deixe isso explícito e peça uma clarificação objetiva.
		""")
	@UserMessage("""
		Pergunta do usuário: {{question}}
		Mês de referência da requisição: {{referenceMonth}}
		Intenção resolvida pelo backend: {{resolvedIntent}}
		Categoria resolvida pelo backend: {{resolvedCategoryName}}
		O household já está isolado no backend.
		Se o mês de referência estiver preenchido, use esse valor e não peça o mês novamente.
		Para qualquer resposta financeira, chame a tool apropriada antes de responder.
		""")
	Result<String> answer(
		@V("question") String question,
		@V("referenceMonth") String referenceMonth,
		@V("resolvedIntent") String resolvedIntent,
		@V("resolvedCategoryName") String resolvedCategoryName
	);
}
