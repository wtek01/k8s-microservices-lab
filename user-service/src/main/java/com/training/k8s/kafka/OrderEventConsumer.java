package com.training.k8s.kafka;

import com.training.k8s.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final UserService userService;

    @KafkaListener(topics = "order-created-topic", groupId = "user-service-group")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event);
        userService.processOrder(event.getUserId(), event.getOrderId());
    }
}