# **Objectif Global**

Mettre en place un projet minimaliste (MVP) de microservices en **Spring Boot** communiquant via **Kafka**, puis ajouter progressivement des fonctionnalités plus avancées (CI/CD GitHub Actions, etc.), le tout déployable sur **Kubernetes**.

---

# **Contexte et Contraintes**

- Je souhaite avoir **deux microservices** (p. ex. `user-service` et `order-service`), tous deux basés sur **Spring Boot** (version 3.x).
- Ces microservices doivent **communiquer via Kafka**.  
  Par exemple, le service `order-service` envoie un message Kafka lors de la création d’une commande, et le `user-service` consomme ou traite cet événement.
- Je veux pouvoir **dockeriser** chaque microservice et les déployer sur **Kubernetes** (via Minikube, K3s ou tout cluster Kubernetes).
- J’ai besoin d’un minimum de **configuration** de Kafka (image Docker, topics, etc.) pour les tests en local.
- Au départ, je souhaite un **MVP** très simple :
    - Structure de projet,
    - 1 ou 2 endpoints REST basiques,
    - Un producteur Kafka d’un côté, un consommateur Kafka de l’autre,
    - Un fichier Dockerfile par microservice,
    - Des manifests Kubernetes (Deployment, Service).
- Ensuite, je souhaite **faire évoluer** ce projet avec des “versions” successives plus abouties (vous pouvez créer des branches ou dossiers séparés) :
    1. **MVP (Version 0.1)** : microservices minimalistes, communication Kafka, dockerisation, déploiement K8s basique.
    2. **Version 0.2** : intégration d’une base de données (PostgreSQL ou autre) et persistance des données, toujours en local ou dans le cluster.
    3. **Version 0.3** : mise en place d’un **pipeline CI/CD** (par exemple GitHub Actions) pour :
        - Construire et tester vos microservices.
        - Créer des images Docker.
        - Pousser ces images vers un registre Docker.
        - Générer/mettre à jour les manifestes Kubernetes.
        - Déployer automatiquement sur votre cluster Kubernetes.
    4. **Version 0.4** :
       a - Ajouter un Ingress Controller pour exposer vos services à l'extérieur du cluster
       b - Mettre en place un monitoring avec Prometheus et Grafana
       c - Mettre en place des tests d'intégration post-déploiement
       d - Implémenter une stratégie de déploiement bleu/vert ou canary pour des mises à jour sans interruption
       e - Configurer une sécurité améliorée comme le mTLS entre services (Bonus)

---

# **Détails et Attendus**

1. **Structure du Projet** :
    - Arborescence du projet :
      ```
      user-service/
        ├─ src/main/java/...
        ├─ pom.xml
        ├─ Dockerfile
        └─ k8s/ (avec les manifests de déploiement)
 
      order-service/
        ├─ src/main/java/...
        ├─ pom.xml
        ├─ Dockerfile
        └─ k8s/
 
      kafka/ (optionnel, config ou docker-compose pour lancer un broker Kafka en local si besoin)
 
      README.md
      ```

2. **Description et Exemple de Code** :
    - **Endpoints REST** :
        - `user-service` : un endpoint `/users` qui retourne une liste d’utilisateurs factices ou permet d’en créer.
        - `order-service` : un endpoint `/orders` pour créer une commande.
    - **Kafka** :
        - Configuration Spring Boot (application.yaml).
        - Un **producteur** dans `order-service` pour émettre un événement `OrderCreated`.
        - Un **consommateur** dans `user-service` qui écoute les événements `OrderCreated`.
    - **Docker** :
        - Un `Dockerfile` pour chaque microservice qui packagera l’application Spring Boot dans une image légère (OpenJDK-alpine ou similaire).
    - **Kubernetes** :
        - Pour chaque microservice, un fichier YAML contenant un **Deployment** et un **Service**.
        - Optionnellement, un **StatefulSet** ou un **Deployment** distinct pour Kafka (ou utilisation d’un opérateur Kafka simplifié, selon vos préférences).
    - **Instructions** :
        - Comment builder et lancer localement (`mvn spring-boot:run` ).
        - Comment builder les images Docker.
        - Comment déployer sur Kubernetes (via `kubectl apply -f ...`).
        - Comment vérifier le fonctionnement (logs, endpoints, etc.).

3. **Processus Incrémental** :
    - **Version 0.1 (MVP)** :
        1. Code minimal, 2 microservices, endpoints REST, un producteur/consommateur Kafka, Dockerfiles, YAML K8s.
        2. Indications pour déployer Kafka (localement via Docker Compose ou sur K8s).
        3. Tests manuels : créer une commande, voir le message Kafka consommé par `user-service`.
    - **Version 0.2** :
        - Ajout d’une base de données (PostgreSQL ou H2) pour stocker les commandes et/ou les utilisateurs.
        - Configuration Docker/Kubernetes pour la base de données.
        - Migration de schéma (Flyway ou Liquibase) en option.
    - **Version 0.3** :
        - Mise en place d’un pipeline CI/CD avec GitHub Actions :
            - Étapes de build (mvn), tests unitaires, construction des images Docker, push sur Docker Hub ou un autre registre.
            - Génération ou application des manifests Kubernetes.
        - Documentation (`README.md` amélioré, ou wiki) expliquant le flux complet.
    - **Version 0.4** :
        - Mise en place d’un Ingress Controller (nginx-ingress ou autre) pour exposer un service en externe.
        - Monitoring basique avec Prometheus/Grafana ou un autre outil.
        - Sécurisation (mTLS, secrets management) si nécessaire.

---

# **Ce que j’aimerais de la part de l’IA**

1. **Code Complet** pour chaque version (ou un exemple de patch/diff entre versions) afin de comprendre comment le projet évolue.
2. **Explications Commentées** : où se trouve le code du producteur Kafka, comment le consommateur est géré, comment configurer Kafka dans `application.yaml`, etc.
3. **Commandes Clés** :
    - Démarrage local,
    - Commandes Docker (build, run, push),
    - Commandes Kubernetes (`kubectl apply`, `kubectl get pods`, etc.).
4. **Options de Personnalisation** :
    - Possibilité de remplacer Kafka par un autre broker (RabbitMQ) si besoin,
    - Possibilité de choisir un orchestrateur local différent (Kind, K3s) si besoin,
    - Etc.

---

# **Format de Réponse Recherché**

- Un guide clair qui présente la **version 0.1** (MVP) en premier, avec tout le code nécessaire.
- Des sections qui décrivent **comment faire évoluer** le projet en versions 0.2, 0.3, etc., avec les nouveaux fichiers ou les modifications requises.
- Idéalement, fournir un lien ou une arborescence claire pour chaque version (même fictive) afin de bien séparer les étapes.

---

**En résumé**, génère un projet (ou squelette) de microservices Spring Boot, communiquant via Kafka, déployable dans Kubernetes, avec des itérations successives pour ajouter des fonctionnalités avancées (base de données, CI/CD, monitoring, etc.). Si une autre approche est plus pertinente ou plus performante, merci de la proposer.
