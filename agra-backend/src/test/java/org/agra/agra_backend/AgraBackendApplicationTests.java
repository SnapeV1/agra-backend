package org.agra.agra_backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
@Disabled("Disabled in CI – requires full infrastructure")
class AgraBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
