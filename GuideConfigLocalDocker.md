# Guide de configuration et d'exécution des microservices

Ce document explique comment configurer et exécuter l'application de microservices dans différents environnements : local (via IDE) et Docker.

## Architecture de l'application

L'application est constituée de plusieurs composants :

- **Kafka** : Broker de messages pour la communication entre les services
- **PostgreSQL** : Base de données relationnelle
- **User Service** : Service de gestion des utilisateurs
- **Order Service** : Service de gestion des commandes

Les services communiquent entre eux via Kafka : lorsqu'une commande est créée, un événement est publié sur Kafka et consommé par le service utilisateur.

## Configuration des environnements

### Profils Spring Boot

L'application utilise les profils Spring Boot pour gérer les différentes configurations :

- **Profil `docker`** : Utilisé lorsque les services sont exécutés dans des conteneurs Docker
- **Profil `local`** : Utilisé lorsque les services sont exécutés localement via l'IDE

Chaque profil a sa propre configuration dans les fichiers suivants :
- `application.yml` : Configuration commune
- `application-docker.yml` : Configuration spécifique à l'environnement Docker
- `application-local.yml` : Configuration spécifique à l'environnement local

### Pourquoi utiliser différentes adresses pour Kafka

Une question fréquente est : "Pourquoi utiliser `localhost:29092` en local et `kafka:9092` dans Docker alors que Kafka s'exécute toujours dans Docker ?"

Voici l'explication :

1. **Réseaux séparés** : Docker crée un réseau isolé où les conteneurs communiquent entre eux en utilisant leurs noms. À l'intérieur de ce réseau, un conteneur peut accéder à Kafka via le nom `kafka` sur le port 9092.

2. **Communication depuis l'extérieur** : Votre machine hôte (où vous exécutez IntelliJ) n'est pas dans ce réseau Docker et ne peut pas résoudre le nom `kafka`. Ce nom n'existe pas dans son système DNS.

3. **Mapping de ports** : Pour permettre l'accès depuis l'extérieur, Docker "expose" des ports en les mappant à des ports de la machine hôte. La configuration `ports: - "29092:9092"` signifie que le port 9092 du conteneur Kafka est accessible via le port 29092 de l'hôte.

4. **Listeners Kafka** : Kafka doit savoir comment il est accessible pour indiquer aux clients comment se connecter. C'est pourquoi on configure deux listeners :
   - `INTERNAL://kafka:9092` (pour les communications entre conteneurs Docker)
   - `EXTERNAL://localhost:29092` (pour les communications depuis l'extérieur)

Si vous tentiez d'utiliser `kafka:9092` depuis votre machine locale, cela échouerait car votre machine ne peut pas résoudre le nom "kafka". C'est comme essayer d'accéder à un site web avec un nom de domaine qui n'existe pas dans le DNS public.

C'est pour cette raison qu'on utilise deux configurations différentes :
- `kafka:9092` dans le profil Docker (à l'intérieur du réseau Docker)
- `localhost:29092` dans le profil Local (depuis votre machine)

### Configuration Docker

Le fichier `application-docker.yml` contient :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/userdb  # ou orderdb
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: kafka:9092
```

Cette configuration utilise les noms d'hôtes `postgres` et `kafka` qui sont résolus dans le réseau Docker.

### Configuration locale

Le fichier `application-local.yml` contient :

```yaml
server:
  port: 8091  # pour user-service (au lieu de 8081)
  # ou port: 8092  # pour order-service (au lieu de 8082)

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb  # ou orderdb
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: user-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.training.k8s.model,com.training.k8s.kafka
```

Cette configuration utilise `localhost` pour accéder aux services d'infrastructure qui s'exécutent dans Docker.

### Configuration Kafka explicite

Pour garantir une connexion correcte à Kafka, une classe de configuration spécifique a été ajoutée :

```java
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.training.k8s.model,com.training.k8s.kafka");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.training.k8s.kafka.OrderCreatedEvent");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

Cette configuration permet de s'assurer que le consommateur Kafka est correctement configuré dans tous les environnements.

## Exécution de l'application

### Exécution en local avec IntelliJ

Pour exécuter les services en local tout en utilisant Kafka et PostgreSQL dans Docker :

1. **Démarrez l'infrastructure dans Docker** :
   ```bash
   docker-compose up -d zookeeper kafka postgres
   ```

2. **Configurez les Run Configurations dans IntelliJ** :
   - Ajoutez l'option VM : `-Dspring.profiles.active=local`
   - Pour user-service, utilisez la classe principale : `com.training.k8s.UserServiceApplication`
   - Pour order-service, utilisez la classe principale : `com.training.k8s.OrderServiceApplication`

3. **Exécutez les services** :
   - Démarrez `UserServiceApplication` dans IntelliJ
   - Démarrez `OrderServiceApplication` dans IntelliJ

4. **Testez l'application** avec Postman ou curl :
   - Créez un utilisateur : `POST http://localhost:8091/users`
   - Créez une commande : `POST http://localhost:8092/orders`
   - Vérifiez que la commande est associée à l'utilisateur : `GET http://localhost:8091/users/{userId}`

### Exécution complète dans Docker

Pour exécuter tous les composants dans Docker :

1. **Construisez et démarrez tous les services** :
   ```bash
   docker-compose up -d
   ```

2. **Vérifiez que tous les services sont opérationnels** :
   ```bash
   docker-compose ps
   ```

3. **Testez l'application** avec Postman ou curl :
   - Créez un utilisateur : `POST http://localhost:8081/users`
   - Créez une commande : `POST http://localhost:8082/orders`
   - Vérifiez que la commande est associée à l'utilisateur : `GET http://localhost:8081/users/{userId}`

## Mode hybride pour le débogage

Pour déboguer un service tout en testant l'intégration avec les autres composants :

1. **Démarrez les services d'infrastructure et le service que vous ne debuggez pas** :
   ```bash
   # Pour déboguer user-service
   docker-compose up -d zookeeper kafka postgres order-service
   
   # Pour déboguer order-service
   docker-compose up -d zookeeper kafka postgres user-service
   ```

2. **Exécutez le service à déboguer dans IntelliJ** avec le profil local

Cette approche vous permet de placer des points d'arrêt et de déboguer un service tout en conservant l'intégration avec le reste du système.

## Problèmes courants et solutions

### Problème de connexion à Kafka

Si vous rencontrez des erreurs de connexion à Kafka comme `No resolvable bootstrap urls`, vérifiez :

1. Que Kafka est bien démarré : `docker-compose ps | grep kafka`
2. Que les ports sont correctement exposés : `docker-compose port kafka 29092`
3. Que la configuration Kafka dans le docker-compose.yml est correcte :
   ```yaml
   KAFKA_LISTENERS: INTERNAL://kafka:9092,EXTERNAL://0.0.0.0:29092
   KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:9092,EXTERNAL://localhost:29092
   KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
   KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
   ```

### Ports déjà utilisés

Si vous recevez une erreur indiquant qu'un port est déjà utilisé, assurez-vous que :

1. Les services Docker n'utilisent pas les mêmes ports que les services locaux
2. Les fichiers de configuration `application-local.yml` utilisent des ports différents :
   - user-service : port 8091 au lieu de 8081
   - order-service : port 8092 au lieu de 8082

## Conclusion

Ce guide vous permet de configurer et d'exécuter l'application de microservices dans différents environnements. En suivant ces instructions, vous pourrez développer, tester et déboguer efficacement votre application, que ce soit en local ou dans des conteneurs Docker.