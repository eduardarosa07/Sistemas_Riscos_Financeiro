package sr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_INTEGRATION", matches = "true")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}