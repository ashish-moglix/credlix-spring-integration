package com.credlix.abfl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@EnableIntegration
@SpringBootApplication
public class AbflApplication {

	public static void main(String[] args) {
		SpringApplication.run(AbflApplication.class, args);
	}

}
 