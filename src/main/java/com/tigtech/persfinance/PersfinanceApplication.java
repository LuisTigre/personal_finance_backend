package com.tigtech.persfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersfinanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersfinanceApplication.class, args);
	}

}
