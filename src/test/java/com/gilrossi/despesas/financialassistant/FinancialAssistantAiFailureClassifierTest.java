package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class FinancialAssistantAiFailureClassifierTest {

	@Test
	void deve_identificar_causa_raiz() {
		RuntimeException cause = new RuntimeException("root");
		RuntimeException wrapper = new RuntimeException("wrapper", new RuntimeException("inner", cause));

		assertThat(FinancialAssistantAiFailureClassifier.rootCause(wrapper)).isSameAs(cause);
	}

	@Test
	void deve_classificar_categorias_principais() throws Exception {
		assertThat(FinancialAssistantAiFailureClassifier.classify(new AuthenticationFailureException()))
			.isEqualTo(FinancialAssistantAiFailureCategory.AUTH_ERROR);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException(new HttpTimeoutException("timeout"))))
			.isEqualTo(FinancialAssistantAiFailureCategory.TIMEOUT);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException(new SocketTimeoutException("timeout"))))
			.isEqualTo(FinancialAssistantAiFailureCategory.TIMEOUT);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException(new TimeoutException("timeout"))))
			.isEqualTo(FinancialAssistantAiFailureCategory.TIMEOUT);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException(new ConnectException("connection refused"))))
			.isEqualTo(FinancialAssistantAiFailureCategory.NETWORK_ERROR);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException(new UnknownHostException("missing"))))
			.isEqualTo(FinancialAssistantAiFailureCategory.NETWORK_ERROR);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException("server_error 500")))
			.isEqualTo(FinancialAssistantAiFailureCategory.PROVIDER_ERROR);
		assertThat(FinancialAssistantAiFailureClassifier.classify(new RuntimeException("anything else")))
			.isEqualTo(FinancialAssistantAiFailureCategory.UNEXPECTED_ERROR);
	}

	private static final class AuthenticationFailureException extends RuntimeException {
		private AuthenticationFailureException() {
			super("authentication failed");
		}
	}
}
