package com.training.k8s.service;

import com.training.k8s.model.User;
import com.training.k8s.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(User user) {
        // Set user ID if not provided
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }

        // Initialize empty order set if null
        if (user.getOrderIds() == null) {
            user.setOrderIds(new java.util.HashSet<>());
        }

        log.info("Creating user: {}", user);
        return userRepository.save(user);
    }

    @Transactional
    public void processOrder(String userId, String orderId) {
        log.info("Processing order: {} for user: {}", orderId, userId);
        userRepository.findById(userId).ifPresentOrElse(
                user -> {
                    user.getOrderIds().add(orderId);
                    userRepository.save(user);
                    log.info("Order added to user's order list: {}", orderId);
                },
                () -> log.warn("User not found with ID: {}", userId)
        );
    }
}