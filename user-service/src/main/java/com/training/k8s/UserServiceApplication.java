package com.training.k8s;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class UserServiceApplication {

	public static void main(String[] args) {
		log.info("========================================");
		log.info("=====User Service Application v1======");
		log.info("========================================");
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
