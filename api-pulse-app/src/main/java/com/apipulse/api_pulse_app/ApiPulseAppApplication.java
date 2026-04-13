package com.apipulse.api_pulse_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiPulseAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiPulseAppApplication.class, args);
	}

}
