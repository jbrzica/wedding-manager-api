package hr.jbrzica.wedding_manager_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WeddingManagerApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
