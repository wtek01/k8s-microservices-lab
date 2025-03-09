package com.training.k8s.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


// This class must match the structure of the event produced by order-service
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private String productId;
    private BigDecimal amount;
    private LocalDateTime orderDate;
}