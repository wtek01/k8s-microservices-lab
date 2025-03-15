# Guide de Référence des Configurations - Projet Microservices Kubernetes

Ce guide offre une référence complète de toutes les configurations utilisées dans le projet de microservices avec CI/CD, en indiquant les fichiers correspondants et leur objectif.

## Organisation du Projet

### Structure des Répertoires
```
project-root/
├── .github/workflows/              # Workflows GitHub Actions
│   ├── main-ci-workflow.yml        # Workflow de build et publication
│   └── kubernetes-deploy-workflow.yml # Workflow de déploiement
├── user-service/                   # Service utilisateur
│   ├── src/                        # Code source Java
│   ├── Dockerfile                  # Configuration Docker
│   └── pom.xml                     # Configuration Maven
├── order-service/                  # Service commande
│   ├── src/                        # Code source Java
│   ├── Dockerfile                  # Configuration Docker
│   └── pom.xml                     # Configuration Maven
├── k8s/                            # Manifestes Kubernetes
│   ├── kafka-statefulset.yml       # Config Kafka
│   ├── postgres-deployment.yml     # Config PostgreSQL
│   ├── user-service.yml            # Config déploiement user-service
│   ├── order-service.yml           # Config déploiement order-service
│   └── kustomization.yml           # Config Kustomize
└── pom.xml                         # POM parent Maven
```

## 1. Configuration Maven

### Fichiers concernés
- `pom.xml` (racine) - Configuration parent
- `user-service/pom.xml` - Configuration service utilisateur
- `order-service/pom.xml` - Configuration service commande

### Rôle
Le POM parent définit les dépendances communes et les versions pour l'ensemble du projet, tandis que les POMs enfants définissent les spécificités de chaque microservice.

### Éléments clés
- Gestion des versions de Spring Boot (3.2.0)
- Dépendances communes (Spring Web, JPA, Kafka, Actuator)
- Configuration des plugins de build

### Exemple
```xml
<!-- Dans pom.xml parent -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <!-- autres dépendances communes -->
</dependencies>
```

## 2. Configuration Spring Boot

### Fichiers concernés
- `user-service/src/main/resources/application.yml`
- `order-service/src/main/resources/application.yml`
- `*/src/main/resources/application-local.yml`
- `*/src/main/resources/application-prod.yml`

### Rôle
Ces fichiers définissent la configuration des applications Spring Boot, y compris les connexions à la base de données, les propriétés Kafka, et les endpoints de monitoring.

### Éléments clés
- Configuration des datasources PostgreSQL
- Configuration des producteurs/consommateurs Kafka
- Configuration des endpoints Actuator
- Profils d'environnement (local, prod)

### Exemple
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:userdb}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
```

## 3. Configuration Kafka

### Fichiers concernés
- `order-service/src/main/java/com/training/k8s/config/KafkaProducerConfig.java`
- `user-service/src/main/java/com/training/k8s/config/KafkaConsumerConfig.java`
- `k8s/kafka-statefulset.yml`

### Rôle
Ces fichiers configurent la communication asynchrone entre les microservices via Kafka, ainsi que le déploiement de Kafka sur Kubernetes.

### Éléments clés
- Configuration du producteur dans order-service
- Configuration du consommateur dans user-service
- Déploiement StatefulSet pour Kafka
- Configuration de Zookeeper

### Exemple
```java
// KafkaProducerConfig.java
@Bean
public KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

## 4. Configuration PostgreSQL

### Fichiers concernés
- `k8s/postgres-deployment.yml`
- `*/src/main/resources/db/migration/V1__*.sql` (fichiers de migration Flyway)

### Rôle
Ces fichiers gèrent le déploiement et la configuration de PostgreSQL dans Kubernetes, ainsi que les migrations de schéma de base de données.

### Éléments clés
- Deployment Kubernetes pour PostgreSQL
- PersistentVolumeClaim pour le stockage persistant
- Secret pour les credentials
- Migrations Flyway pour l'initialisation et les évolutions de schéma

### Exemple
```yaml
# postgres-deployment.yml (extrait)
apiVersion: v1
kind: Secret
metadata:
  name: postgres-credentials
type: Opaque
data:
  username: cG9zdGdyZXM=  # postgres en base64
  password: cG9zdGdyZXM=  # postgres en base64
```

## 5. Configuration Docker

### Fichiers concernés
- `user-service/Dockerfile`
- `order-service/Dockerfile`

### Rôle
Ces fichiers définissent comment empaqueter les applications Spring Boot dans des conteneurs Docker.

### Éléments clés
- Build multi-stage pour optimiser la taille
- Utilisation de JRE Alpine pour minimiser l'empreinte
- Configuration sécurisée avec un utilisateur non-root

### Exemple
```dockerfile
# Étape de construction
FROM eclipse-temurin:17-jdk-alpine as build
...
# Étape de déploiement
FROM eclipse-temurin:17-jre-alpine
...
# Utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

## 6. Configuration Kubernetes

### Fichiers concernés
- `k8s/kustomization.yml`
- `k8s/user-service.yml`
- `k8s/order-service.yml`
- `k8s/kafka-statefulset.yml`
- `k8s/postgres-deployment.yml`

### Rôle
Ces fichiers définissent le déploiement et la configuration des services dans Kubernetes.

### Éléments clés
- Deployments pour les services
- Configuration des ressources (CPU/mémoire)
- Health checks (liveness/readiness probes)
- Configuration des variables d'environnement
- Services pour l'exposition des applications

### Exemple
```yaml
# user-service.yml (extrait)
spec:
  containers:
    - name: user-service
      image: wtek01/user-service:0.3.0
      ports:
        - containerPort: 8081
      livenessProbe:
        httpGet:
          path: /actuator/health
          port: 8081
```

## 7. Configuration CI/CD GitHub Actions

### Fichiers concernés
- `.github/workflows/main-ci-workflow.yml`
- `.github/workflows/kubernetes-deploy-workflow.yml`

### Rôle
Ces fichiers définissent les pipelines d'intégration continue et de déploiement continu.

### Éléments clés
- Configuration des déclencheurs (push, workflow_run)
- Étapes de build et test
- Construction et push des images Docker
- Déploiement sur Kubernetes
- Gestion des secrets

### Exemple
```yaml
# kubernetes-deploy-workflow.yml (extrait)
on:
  workflow_run:
    workflows: ["Build and Publish"]
    types:
      - completed
jobs:
  deploy:
    runs-on: self-hosted
    steps:
      - name: Apply Kubernetes manifests
        run: |
          kubectl apply -f k8s/kafka-statefulset.yml
          kubectl apply -f k8s/postgres-deployment.yml
          kubectl apply -f k8s/user-service.yml
          kubectl apply -f k8s/order-service.yml
```

## 8. Configuration du Runner Auto-hébergé

### Fichiers concernés
- `.github/workflows/kubernetes-deploy-workflow.yml` (configuration spécifique à Windows)

### Rôle
Permet l'exécution des workflows GitHub Actions sur une machine locale avec accès au cluster Kubernetes.

### Éléments clés
- Spécification de `runs-on: self-hosted`
- Utilisation de `shell: powershell` pour les commandes
- Adaptation des commandes bash pour PowerShell
- Gestion du kubeconfig

### Exemple
```yaml
# kubernetes-deploy-workflow.yml (extrait)
- name: Setup kubeconfig
  shell: powershell
  run: |
    if (!(Test-Path -Path "$HOME/.kube")) {
      New-Item -Path "$HOME/.kube" -ItemType Directory
    }
    [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("${{ secrets.KUBE_CONFIG }}")) | Out-File -FilePath "$HOME/.kube/config" -Encoding utf8
```

## 9. Secrets et Variables d'Environnement

### Emplacements concernés
- GitHub Repository → Settings → Secrets and variables → Actions
- Manifestes Kubernetes (références aux secrets)

### Rôle
Stockage sécurisé des informations sensibles et injection dans les workflows et applications.

### Secrets principaux
- `DOCKERHUB_USERNAME` - Nom d'utilisateur Docker Hub
- `DOCKERHUB_TOKEN` - Token d'accès personnel Docker Hub
- `KUBE_CONFIG` - Configuration Kubernetes encodée en base64

### Exemple d'utilisation
```yaml
# Dans les workflows GitHub Actions
with:
  username: ${{ secrets.DOCKERHUB_USERNAME }}
  password: ${{ secrets.DOCKERHUB_TOKEN }}

# Dans les manifestes Kubernetes
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: postgres-credentials
        key: password
```

## 10. Résumé des Interactions entre Configurations

Les différentes configurations s'articulent entre elles pour former un système cohérent :

1. **Maven** compile le code Java et génère les JARs
2. **Docker** empaquette ces JARs dans des images conteneurisées
3. **GitHub Actions** orchestre la construction et le déploiement
4. **Kubernetes** gère le cycle de vie des conteneurs
5. **Spring Boot** configure les applications pour communiquer entre elles et avec Kafka/PostgreSQL

Ce système de configuration complet permet un déploiement entièrement automatisé des microservices, depuis le code source jusqu'à l'exécution dans Kubernetes.