# Guide de déploiement et test des microservices
## Du développement local au déploiement Kubernetes automatisé

Ce guide détaille les étapes pour configurer, exécuter et tester le projet de microservices à chaque niveau de déploiement : en local, avec Docker, et enfin via le déploiement automatisé sur Kubernetes.

## Table des matières
1. [Prérequis](#prérequis)
2. [Configuration initiale](#configuration-initiale)
3. [Exécution et test en local](#exécution-et-test-en-local)
4. [Exécution et test avec Docker](#exécution-et-test-avec-docker)
5. [Déploiement automatisé sur Kubernetes](#déploiement-automatisé-sur-kubernetes)
6. [Troubleshooting](#troubleshooting)

## Prérequis

Pour suivre ce guide, vous aurez besoin des outils suivants installés sur votre machine:

- **Développement:**
    - JDK 17
    - Maven 3.6+
    - IDE (IntelliJ IDEA, Eclipse, VS Code)
    - Git

- **Docker:**
    - Docker Desktop ou Docker Engine + Docker Compose

- **Kubernetes:**
    - Minikube
    - kubectl
    - GitHub Actions runner auto-hébergé (pour le déploiement automatisé)

## Configuration initiale

### 1. Clonage du dépôt

```bash
# Cloner le dépôt Git
git clone https://github.com/votre-nom/k8s-microservices-lab.git
cd k8s-microservices-lab
```

### 2. Configuration de la base de données locale

```bash
# Créer les bases de données PostgreSQL nécessaires
psql -U postgres -c "CREATE DATABASE userdb;"
psql -U postgres -c "CREATE DATABASE orderdb;"
```

### 3. Configuration des variables d'environnement (optionnel)

Créez un fichier `.env` à la racine du projet:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
```

## Exécution et test en local

Pour exécuter et tester le projet en local, vous devez lancer les composants d'infrastructure (Kafka, PostgreSQL) puis les microservices.

### 1. Lancement de l'infrastructure locale

#### Démarrer Kafka et Zookeeper

La façon la plus simple est d'utiliser Docker Compose pour Kafka et Zookeeper:

Créez un fichier `docker-compose-local.yml` à la racine du projet:

```yaml
version: '3'
services:
  zookeeper:
    image: bitnami/zookeeper:latest
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
    ports:
      - "2181:2181"

  kafka:
    image: bitnami/kafka:latest
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,OUTSIDE://:29092
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,OUTSIDE://localhost:29092
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,OUTSIDE:PLAINTEXT
      - ALLOW_PLAINTEXT_LISTENER=yes
      - KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true
    ports:
      - "29092:29092"
    depends_on:
      - zookeeper

  postgres:
    image: postgres:14-alpine
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=postgres
    ports:
      - "5432:5432"
    volumes:
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
```

Créez un fichier `init-db.sql`:

```sql
CREATE DATABASE userdb;
CREATE DATABASE orderdb;
```

Lancez l'infrastructure:

```bash
docker-compose -f docker-compose-local.yml up -d
```

### 2. Compilation du projet

```bash
# Compiler l'ensemble du projet avec Maven
mvn clean install -DskipTests
```

### 3. Lancement des microservices

#### Lancer le service Utilisateur

```bash
# Dans un terminal
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Ou Sur Intellij on peut ajouter SPRING_PROFILES_ACTIVE=local dans Run / Edit Configuration
#### Lancer le service Commande

```bash
# Dans un autre terminal
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Test des services en local

#### Test du service Utilisateur

```bash
# Créer un utilisateur
curl -X POST http://localhost:8091/users -H "Content-Type: application/json" -d '{
  "name": "John Doe",
  "email": "john.doe@example.com"
}'

# Récupérer la liste des utilisateurs
curl http://localhost:8091/users
```

#### Test du service Commande

```bash
# Utiliser l'ID de l'utilisateur créé précédemment
USER_ID="id-de-utilisateur-retourné"

# Créer une commande
curl -X POST http://localhost:8092/orders -H "Content-Type: application/json" -d '{
  "userId": "'$USER_ID'",
  "productId": "prod-001",
  "amount": 99.99
}'

# Récupérer la liste des commandes
curl http://localhost:8092/orders
```

#### Vérifier l'intégration Kafka

```bash
# Vérifier que la commande a bien été ajoutée à l'utilisateur
curl http://localhost:8091/users/$USER_ID

# La réponse devrait inclure la commande dans la liste orderIds
```

## Exécution et test avec Docker

Pour exécuter et tester le projet entièrement avec Docker, nous utiliserons Docker Compose pour créer un environnement intégré.

### 1. Construction des images Docker
  - Normalement c'est configuré dnas docker-compose.yml 
```bash
# Construction des images pour chaque service
docker build -t votre-nom/user-service:latest -f user-service/Dockerfile .
docker build -t votre-nom/order-service:latest -f order-service/Dockerfile .
```

### 2. Création du fichier Docker Compose

Créez un fichier `docker-compose.yml` à la racine du projet:

```yaml
version: '3'
services:
  zookeeper:
    image: bitnami/zookeeper:latest
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
    ports:
      - "2181:2181"

  kafka:
    image: bitnami/kafka:latest
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - ALLOW_PLAINTEXT_LISTENER=yes
      - KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  postgres:
    image: postgres:14-alpine
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_MULTIPLE_DATABASES=userdb,orderdb
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql

  user-service:
    image: votre-nom/user-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_USER=postgres
      - DB_PASSWORD=postgres
      - DB_NAME=userdb
    ports:
      - "8091:8081"
    depends_on:
      - postgres
      - kafka

  order-service:
    image: votre-nom/order-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_USER=postgres
      - DB_PASSWORD=postgres
      - DB_NAME=orderdb
    ports:
      - "8092:8082"
    depends_on:
      - postgres
      - kafka
      - user-service

volumes:
  postgres-data:
```

### 3. Lancement de l'environnement Docker

```bash
# Lancer tous les services
docker-compose up -d

# Vérifier que tous les conteneurs sont en fonctionnement
docker-compose ps
```

### 4. Test des services dans Docker

Utilisez les mêmes commandes curl que pour les tests locaux, les services étant exposés sur les mêmes ports:

```bash
# Test du service utilisateur
curl -X POST http://localhost:8091/users -H "Content-Type: application/json" -d '{
  "name": "Jane Smith",
  "email": "jane.smith@example.com"
}'

# Test du service commande
USER_ID="id-de-utilisateur-retourné"
curl -X POST http://localhost:8092/orders -H "Content-Type: application/json" -d '{
  "userId": "'$USER_ID'",
  "productId": "prod-002",
  "amount": 149.99
}'
```

### 5. Surveillance des logs

```bash
# Afficher les logs des services
docker-compose logs -f user-service
docker-compose logs -f order-service
```

### 6. Nettoyage

```bash
# Arrêter tous les conteneurs
docker-compose down

# Pour supprimer également les volumes
docker-compose down -v
```

## Déploiement automatisé sur Kubernetes

Pour mettre en place le déploiement automatisé sur Kubernetes, vous devez configurer Minikube, le GitHub Actions runner, et les secrets nécessaires.

### 1. Configuration de Minikube

```bash
# Démarrer Minikube avec suffisamment de ressources
minikube start --cpus 4 --memory 8192 --disk-size 20g

# Vérifier que Minikube fonctionne
kubectl get nodes
```

### 2. Configuration du GitHub Actions runner

#### Installation du runner

1. Dans votre dépôt GitHub, allez dans `Settings` > `Actions` > `Runners`
2. Cliquez sur `New self-hosted runner`
3. Suivez les instructions pour installer le runner sur votre machine
4. Configurez-le pour être exécuté comme un service

```powershell
# Sur Windows, dans PowerShell avec droits administrateur
# Dans le répertoire du runner
./config.cmd --url https://github.com/votre-nom/k8s-microservices-lab --token TOKEN_FOURNI
./svc.ps1 install
./svc.ps1 start
```

### 3. Configuration des secrets GitHub

Dans votre dépôt GitHub, allez dans `Settings` > `Secrets and variables` > `Actions` et ajoutez ces secrets:

- `DOCKERHUB_USERNAME`: votre nom d'utilisateur Docker Hub
- `DOCKERHUB_TOKEN`: un token d'accès Docker Hub (pas votre mot de passe)
- `KUBE_CONFIG`: le contenu de votre fichier kubeconfig (obtenu via `cat ~/.kube/config | base64`)

### 4. Déploiement initial de l'infrastructure Kubernetes

```bash
# Créer le secret PostgreSQL
kubectl create secret generic postgres-credentials \
  --from-literal=username=postgres \
  --from-literal=password=postgres

# Déployer l'infrastructure de base
kubectl apply -f k8s/kafka-statefulset.yml
kubectl apply -f k8s/postgres-deployment.yml
```

### 5. Configuration des fichiers de workflow GitHub Actions

Assurez-vous que les fichiers suivants existent dans le dépôt:

- `.github/workflows/main-ci-workflow.yml`
- `.github/workflows/kubernetes-deploy-workflow.yml`

### 6. Déclencher le premier déploiement

Pour déclencher le déploiement automatique complet, faites une modification dans le code:

```bash
# Modifier un fichier source
# Par exemple, ajouter un log dans UserServiceApplication.java
git add .
git commit -m "feat: add startup log message"
git push
```

### 7. Suivi du déploiement

1. Dans GitHub, allez dans l'onglet `Actions` pour suivre l'exécution des workflows
2. Vérifiez les logs dans le workflow "Build and Publish", puis dans "Deploy to Kubernetes"
3. Une fois le déploiement terminé, vérifiez l'état des pods:

```bash
kubectl get pods
```

### 8. Test des services déployés

Pour accéder aux services déployés sur Kubernetes:

```bash
# Utilisez port-forward pour exposer les services
kubectl port-forward svc/user-service 8091:8081 &
kubectl port-forward svc/order-service 8092:8082 &

# Testez les services comme précédemment
curl http://localhost:8091/users
curl http://localhost:8092/orders
```

### 9. Mise à jour et redéploiement automatique

Pour tester le processus complet d'intégration continue et de déploiement continu:

1. Faites une modification dans le code source
2. Commitez et poussez les changements
3. Observez le déclenchement automatique des workflows
4. Vérifiez que les nouveaux pods sont déployés avec vos modifications

```bash
# Vérifiez la version des images utilisées
kubectl describe pod -l app=user-service | grep Image
kubectl describe pod -l app=order-service | grep Image
```

## Troubleshooting

### Problèmes courants en local

| Problème | Solution |
|----------|----------|
| Port déjà utilisé | Modifiez le port dans `application-local.yml` |
| Échec de connexion à PostgreSQL | Vérifiez que PostgreSQL est en cours d'exécution et les identifiants |
| Échec de connexion à Kafka | Vérifiez que Kafka est en cours d'exécution et accessible |

### Problèmes courants avec Docker

| Problème | Solution |
|----------|----------|
| Échec de construction d'image | Vérifiez le Dockerfile et les dépendances |
| Conteneur s'arrêtant immédiatement | Examinez les logs avec `docker logs` |
| Problèmes de réseau | Vérifiez que les services peuvent se voir via leurs noms |

### Problèmes courants avec Kubernetes

| Problème | Solution |
|----------|----------|
| Échec du déploiement | Utilisez `kubectl describe pod <pod-name>` pour voir les détails |
| Échec des sondes de disponibilité | Ajustez les délais dans les manifestes Kubernetes |
| Échec du pipeline CI/CD | Vérifiez les logs GitHub Actions et les secrets configurés |
| Problèmes de kubeconfig | Régénérez et mettez à jour le secret `KUBE_CONFIG` |

### Commandes utiles pour le diagnostic

```bash
# Vérifier les logs d'un service
kubectl logs deployment/user-service
kubectl logs deployment/order-service

# Exécuter un shell dans un pod pour le diagnostic
kubectl exec -it deployment/user-service -- /bin/sh

# Vérifier les événements Kubernetes
kubectl get events --sort-by=.metadata.creationTimestamp

# Redémarrer un déploiement
kubectl rollout restart deployment/user-service
```

---

Ce guide couvre l'ensemble du processus, du développement local au déploiement automatisé sur Kubernetes. En suivant ces étapes, vous pourrez développer, tester et déployer votre application de microservices de manière efficace et cohérente.