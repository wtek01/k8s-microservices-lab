package com.training.k8s;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // Utilise le profil "test" pour les tests
public class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
        // Teste simplement que le contexte Spring se charge correctement
    }
}