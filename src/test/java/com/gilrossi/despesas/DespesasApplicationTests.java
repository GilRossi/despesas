package com.gilrossi.despesas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "app.security.token-secret=test-api-token-secret-for-tests")
class DespesasApplicationTests {

	@Test
	void contextLoads() {
	}

}
