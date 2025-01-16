package com.credlix.abfl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

/**
 * spring integration to intercept emails and attachments on outlook 365 account
 */
@EnableIntegration
@SpringBootApplication
public class AbflApplication {

	public static void main(String[] args) {
		SpringApplication.run(AbflApplication.class, args);
	}

}
