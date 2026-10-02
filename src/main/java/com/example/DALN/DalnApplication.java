package com.example.DALN;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DalnApplication {

	public static void main(String[] args) {
		SpringApplication.run(DalnApplication.class, args);
	}

}
