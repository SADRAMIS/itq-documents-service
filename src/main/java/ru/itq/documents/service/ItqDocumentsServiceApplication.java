package ru.itq.documents.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItqDocumentsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItqDocumentsServiceApplication.class, args);
	}

}
