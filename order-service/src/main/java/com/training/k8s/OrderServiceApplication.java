package com.training.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {
	public static void main(String[] args) {
		System.out.println("========================================");
		System.out.println("DÉMARRAGE AVEC CODE MODIFIÉ - VERSION XYZ");
		System.out.println("========================================");
		SpringApplication.run(OrderServiceApplication.class, args);
	}
}
