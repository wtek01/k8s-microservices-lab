package com.training.k8s.service;

import com.training.k8s.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    // In-memory storage for the MVP
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserService() {
        // Adding some dummy users for testing
        User user1 = User.builder()
                .id(UUID.randomUUID().toString())
                .name("John Doe")
                .email("john.doe@example.com")
                .build();
        
        User user2 = User.builder()
                .id(UUID.randomUUID().toString())
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .build();
        
        users.put(user1.getId(), user1);
        users.put(user2.getId(), user2);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> getUserById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public User createUser(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        users.put(user.getId(), user);
        return user;
    }

    public void processOrder(String userId, String orderId) {
        System.out.println("Processing order " + orderId + " for user " + userId);
        // In a real application, we would update user's order history, send notifications, etc.
    }
}