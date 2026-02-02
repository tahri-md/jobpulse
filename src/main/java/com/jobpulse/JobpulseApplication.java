package com.jobpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
@EnableScheduling
@SpringBootApplication
public class JobpulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobpulseApplication.class, args);
	}

}
