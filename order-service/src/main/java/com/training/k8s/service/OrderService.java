package com.training.k8s.service;

import com.training.k8s.model.Order;
import com.training.k8s.model.OrderCreatedEvent;
import com.training.k8s.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public Order createOrder(Order order) {
        log.info("Creating order test: {}", order);

        // Set order ID and date if not provided
        if (order.getId() == null) {
            order.setId(UUID.randomUUID().toString());
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }

        // Save the order to database
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getId());

        try {
            // Create and send Kafka event
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .productId(savedOrder.getProductId())
                    .amount(savedOrder.getAmount())
                    .orderDate(savedOrder.getOrderDate())
                    .build();

            kafkaProducerService.sendOrderCreatedEvent(event);
            log.info("Event sent to Kafka for order ID: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("Error sending event to Kafka: {}", e.getMessage(), e);
        }

        return savedOrder;
    }
}