package com.training.k8s;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class OrderServiceApplication {
	public static void main(String[] args) {
		log.info("========================================");
		log.info("Order Service Application v3");
		log.info("========================================");
		SpringApplication.run(OrderServiceApplication.class, args);
	}
}
