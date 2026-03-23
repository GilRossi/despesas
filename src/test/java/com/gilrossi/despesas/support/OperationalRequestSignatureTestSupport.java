package com.gilrossi.despesas.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.gilrossi.despesas.security.OperationalRequestSignatureSupport;

public final class OperationalRequestSignatureTestSupport {

	public static final String TEST_KEY_ID = "test-operational-key";
	public static final String TEST_SECRET = "test-operational-secret";

	private OperationalRequestSignatureTestSupport() {
	}

	public static MockHttpServletRequestBuilder signedOperationalPost(String path, String payload) {
		return signedOperationalPost(path, payload, TEST_KEY_ID, TEST_SECRET, Instant.now().getEpochSecond(), UUID.randomUUID().toString());
	}

	public static MockHttpServletRequestBuilder signedOperationalPost(
		String path,
		String payload,
		String keyId,
		String secret,
		long timestampSeconds,
		String nonce
	) {
		String bodyHash = OperationalRequestSignatureSupport.sha256Hex(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		String signature = OperationalRequestSignatureSupport.hmacSha256Hex(
			secret,
			OperationalRequestSignatureSupport.canonicalPayload(
				"POST",
				path,
				keyId,
				Long.toString(timestampSeconds),
				nonce,
				bodyHash
			)
		);
		return post(path)
			.contentType(MediaType.APPLICATION_JSON)
			.content(payload)
			.header(OperationalRequestSignatureSupport.KEY_ID_HEADER, keyId)
			.header(OperationalRequestSignatureSupport.TIMESTAMP_HEADER, Long.toString(timestampSeconds))
			.header(OperationalRequestSignatureSupport.NONCE_HEADER, nonce)
			.header(OperationalRequestSignatureSupport.BODY_SHA256_HEADER, bodyHash)
			.header(OperationalRequestSignatureSupport.SIGNATURE_HEADER, signature);
	}
}
