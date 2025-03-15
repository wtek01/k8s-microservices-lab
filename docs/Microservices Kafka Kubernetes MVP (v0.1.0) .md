# Microservices Kafka Kubernetes MVP (v0.1.0)

Ce projet est un MVP (Minimum Viable Product) démontrant une architecture de microservices avec Spring Boot, Kafka et Kubernetes.

## Architecture

Le MVP comprend les composants suivants :
- **user-service** : Microservice de gestion des utilisateurs avec un consommateur Kafka
- **order-service** : Microservice de gestion des commandes avec un producteur Kafka
- **kafka** : Broker de messages utilisé pour la communication entre microservices

## Prérequis

- JDK 17+
- Maven 3.6+
- Docker et Docker Compose
- kubectl
- Un cluster Kubernetes (Minikube, K3s, ou autre)

## Structure du projet

```
microservices-kafka-k8s/
├── pom.xml                 # POM parent pour gérer les dépendances communes
├── docker-compose.yml      # Configuration Docker Compose pour l'ensemble du système
├── user-service/           # Service utilisateur avec consommateur Kafka
│   ├── src/                # Code source
│   ├── pom.xml             # Configuration Maven spécifique au service
│   └── Dockerfile          # Image Docker pour le service
├── order-service/          # Service commande avec producteur Kafka
│   ├── src/                # Code source
│   ├── pom.xml             # Configuration Maven spécifique au service
│   └── Dockerfile          # Image Docker pour le service
└── k8s/                    # Manifestes Kubernetes pour tous les composants
    ├── kafka-deployment.yml # Configuration Kafka pour Kubernetes
    ├── user-service.yml    # Déploiement du service utilisateur
    ├── order-service.yml   # Déploiement du service commande
    └── kustomization.yaml  # Configuration Kustomize pour déployer ensemble
```

## Instructions

### 1. Compilation locale

Vous pouvez compiler tous les services à partir du POM parent :

```bash
# Depuis la racine du projet
./mvnw clean package
```

Ou individuellement pour chaque service :

```bash
cd user-service
./mvnw clean package
cd ../order-service
./mvnw clean package
```

### 2. Exécution locale (sans Docker)

Vous devez d'abord lancer Kafka localement (avec Docker Compose) :

```bash
cd kafka
docker-compose up -d zookeeper kafka
```

Puis lancez chaque service dans un terminal différent :

```bash
cd user-service
./mvnw spring-boot:run
```

```bash
cd order-service
./mvnw spring-boot:run
```

### 3. Construction des images Docker

Pour chaque service :

```bash
cd user-service
docker build -t user-service:0.1.0 .
cd ../order-service
docker build -t order-service:0.1.0 .
```

### 4. Exécution avec Docker Compose

```bash
# Depuis la racine du projet
docker-compose up -d
```

### 5. Déploiement sur Kubernetes

Déployez tous les services ensemble avec kustomize :

```bash
kubectl apply -k k8s/
```

Ou déployez-les individuellement :

```bash
kubectl apply -f k8s/kafka-deployment.yml
kubectl apply -f k8s/user-service.yml
kubectl apply -f k8s/order-service.yml
```

Vérifiez que tous les pods sont en cours d'exécution :

```bash
kubectl get pods
```

## Test du système

### 1. Création d'un utilisateur

```bash
curl -X POST http://localhost:8081/users -H "Content-Type: application/json" -d '{"name":"Test User","email":"test@example.com"}'
```

### 2. Création d'une commande

```bash
curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d '{"userId":"USER_ID_FROM_STEP_1","productId":"product123","amount":99.99}'
```

### 3. Vérification des logs

Vérifiez les logs du service utilisateur pour confirmer la réception de l'événement :

```bash
# Local
tail -f user-service/logs/app.log

# Docker
docker logs user-service

# Kubernetes
kubectl logs deployment/user-service
```

Vous devriez voir un message indiquant que l'événement OrderCreated a été reçu.

## Points d'attention pour le MVP

1. Cette version utilise un stockage en mémoire (pas de base de données persistante)
2. La configuration de sécurité est minimale
3. Il n'y a pas de gestion d'erreurs avancée ni de retry mechanism

## Prochaines étapes (v0.2.0)

Dans la prochaine version, nous ajouterons :
- Intégration d'une base de données (PostgreSQL)
- Persistance des données
- Gestion avancée des erreurs