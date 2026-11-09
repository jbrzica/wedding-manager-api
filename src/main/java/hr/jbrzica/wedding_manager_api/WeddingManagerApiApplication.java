package hr.jbrzica.wedding_manager_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Za noćno pražnjenje koša (KosService)
@EnableScheduling
public class WeddingManagerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeddingManagerApiApplication.class, args);
	}

}
