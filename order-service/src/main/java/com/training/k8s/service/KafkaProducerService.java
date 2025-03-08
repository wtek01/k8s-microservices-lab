package com.training.k8s.service;

import com.training.k8s.model.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC_ORDER_CREATED = "order-created-topic";
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending OrderCreatedEvent to topic {}: {}", TOPIC_ORDER_CREATED, event);
        kafkaTemplate.send(TOPIC_ORDER_CREATED, event.getOrderId(), event);
    }
}