package com.gilrossi.despesas.financialassistant;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

public final class FinancialAssistantAiFailureClassifier {

	private FinancialAssistantAiFailureClassifier() {
	}

	public static FinancialAssistantAiFailureCategory classify(Throwable throwable) {
		Throwable rootCause = rootCause(throwable);
		String simpleName = rootCause.getClass().getSimpleName();
		String message = rootCause.getMessage() == null ? "" : rootCause.getMessage().toLowerCase();

		if (simpleName.contains("Authentication")) {
			return FinancialAssistantAiFailureCategory.AUTH_ERROR;
		}
		if (rootCause instanceof HttpTimeoutException
			|| rootCause instanceof SocketTimeoutException
			|| rootCause instanceof TimeoutException
			|| simpleName.contains("Timeout")) {
			return FinancialAssistantAiFailureCategory.TIMEOUT;
		}
		if (rootCause instanceof ConnectException
			|| rootCause instanceof UnknownHostException
			|| rootCause instanceof NoRouteToHostException
			|| rootCause instanceof SocketException
			|| message.contains("connection refused")
			|| message.contains("connection reset")
			|| message.contains("network is unreachable")
			|| message.contains("failed to connect")) {
			return FinancialAssistantAiFailureCategory.NETWORK_ERROR;
		}
		if (simpleName.contains("ServerException")
			|| simpleName.contains("RateLimit")
			|| message.contains("server_error")
			|| message.contains("\"error\"")
			|| message.contains("status code 5")) {
			return FinancialAssistantAiFailureCategory.PROVIDER_ERROR;
		}
		return FinancialAssistantAiFailureCategory.UNEXPECTED_ERROR;
	}

	public static Throwable rootCause(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}
}
