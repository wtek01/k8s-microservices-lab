package com.training.k8s.service;

import com.training.k8s.model.Order;
import com.training.k8s.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final KafkaProducerService kafkaProducerService;

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public Optional<Order> getOrderById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order createOrder(Order order) {
        log.info("Creating order...");
        // Code existant pour créer la commande

        try {
            log.info("Preparing to send Kafka event for order: {}", order.getId());
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    // Construction de l'événement
                    .build();

            if (kafkaProducerService == null) {
                log.error("KafkaProducerService is null - bean not injected!");
            } else {
                try {
                    kafkaProducerService.sendOrderCreatedEvent(event);
                } catch (Exception e) {
                    log.error("Error sending Kafka event", e);
                }
            }
        } catch (Exception e) {
            log.error("Error preparing Kafka event", e);
        }

        return order;
    }
}