package com.gilrossi.despesas.financialassistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;

import dev.langchain4j.service.V;

class FinancialAssistantAiServiceContractTest {

	@Test
	void deve_anotar_variaveis_do_template_explictamente() throws NoSuchMethodException {
		Method method = FinancialAssistantAiService.class.getMethod(
			"answer",
			String.class,
			String.class,
			String.class,
			String.class
		);

		assertTemplateVariable(method.getParameters()[0], "question");
		assertTemplateVariable(method.getParameters()[1], "referenceMonth");
		assertTemplateVariable(method.getParameters()[2], "resolvedIntent");
		assertTemplateVariable(method.getParameters()[3], "resolvedCategoryName");
	}

	private void assertTemplateVariable(Parameter parameter, String expectedName) {
		V annotation = parameter.getAnnotation(V.class);
		assertThat(annotation)
			.as("parameter %s should declare @V", expectedName)
			.isNotNull();
		assertThat(annotation.value()).isEqualTo(expectedName);
	}
}
