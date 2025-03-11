# Guide Kubernetes pour le Projet Microservices avec Kafka

Ce guide couvre la configuration, le déploiement et la gestion de notre architecture de microservices Spring Boot avec Kafka sur Kubernetes.

## Table des matières
1. [Architecture du projet](#architecture-du-projet)
2. [Installation et configuration](#installation-et-configuration)
3. [Préparation des images Docker](#préparation-des-images-docker)
4. [Déploiement sur Kubernetes](#déploiement-sur-kubernetes)
5. [Test et validation](#test-et-validation)
6. [Commandes essentielles](#commandes-essentielles)
7. [Troubleshooting](#troubleshooting)

## Architecture du projet

Notre application MVP est composée de :

- **User Service** : microservice gérant les utilisateurs, consommateur Kafka
- **Order Service** : microservice gérant les commandes, producteur Kafka
- **Kafka & Zookeeper** : broker de messages pour la communication asynchrone

![Architecture du projet](https://miro.medium.com/max/1400/1*Dn4sIB9lN8IcyYbmVnmuhQ.png)

## Installation et configuration

### Installation de Minikube

Minikube est un outil qui permet d'exécuter Kubernetes localement.

**Sur Windows :**
```powershell
# Avec Chocolatey
choco install minikube

# Ou télécharger directement l'exécutable
# https://minikube.sigs.k8s.io/docs/start/
```

### Installation de kubectl

kubectl est l'outil CLI pour interagir avec un cluster Kubernetes.

**Sur Windows :**
```powershell
# Avec Chocolatey
choco install kubernetes-cli

# Ou télécharger directement l'exécutable
# https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/
```

### Démarrage du cluster Minikube

```bash
# Démarrer Minikube avec Docker comme driver
minikube start --driver=docker

# Vérifier l'état
minikube status

# Vérifier que kubectl est correctement configuré
kubectl cluster-info
```

## Préparation des images Docker

Pour notre projet, nous devons construire deux images Docker : `user-service` et `order-service`.

### Construction des images

```bash
# Se placer à la racine du projet
cd microservices-kafka-k8s

# Construire l'image pour user-service
docker build -t user-service:0.1.0 -f user-service/Dockerfile.alt .

# Construire l'image pour order-service
docker build -t order-service:0.1.0 -f order-service/Dockerfile.alt .
```

### Charger les images dans Minikube

Puisque Minikube utilise un daemon Docker distinct, nous devons charger nos images locales dans Minikube.

```bash
# Charger les images dans Minikube
minikube image load user-service:0.1.0
minikube image load order-service:0.1.0

# Vérifier que les images sont disponibles dans Minikube
minikube ssh -- docker images
```

## Déploiement sur Kubernetes

### Structure des manifestes Kubernetes

Notre projet utilise les manifestes Kubernetes suivants :

```
k8s/
├── kafka-deployment.yml  # Déploiement de Kafka et Zookeeper
├── user-service.yml      # Déploiement du service utilisateur
├── order-service.yml     # Déploiement du service commande
└── kustomization.yaml    # Configuration Kustomize
```

### 1. Déploiement de Kafka et Zookeeper

```bash
kubectl apply -f k8s/kafka-deployment.yml

# Vérifier que les pods Kafka et Zookeeper sont en cours d'exécution
kubectl get pods -w
```

> **Important** : Attendez que Kafka et Zookeeper soient dans l'état "Running" avant de continuer.

### 2. Déploiement des microservices

```bash
# Déployer les deux services
kubectl apply -f k8s/user-service.yml
kubectl apply -f k8s/order-service.yml

# Ou déployer tous les composants en utilisant kustomize
kubectl apply -k k8s/
```

### 3. Vérifier le déploiement

```bash
# Lister tous les pods
kubectl get pods

# Lister tous les services
kubectl get services

# Voir les détails du déploiement
kubectl get all
```

## Test et validation

### 1. Accès aux services

Kubernetes ne rend pas automatiquement les services accessibles depuis l'extérieur. Utilisez port-forward pour y accéder :

```bash
# Port-forward pour user-service
kubectl port-forward svc/user-service 8081:8081

# Dans un autre terminal, port-forward pour order-service
kubectl port-forward svc/order-service 8082:8082
```

### 2. Test des API REST

```bash
# Lister les utilisateurs
curl http://localhost:8081/users

# Créer une commande (ce qui déclenchera un événement Kafka)
curl -X POST http://localhost:8082/orders \
    -H "Content-Type: application/json" \
    -d '{"userId":"USER_ID_HERE","productId":"product123","amount":99.99}'
```

### 3. Vérification des logs

Pour vérifier si l'événement Kafka a été correctement produit et consommé :

```bash
# Voir les logs du service user
kubectl logs deployment/user-service

# Voir les logs du service order
kubectl logs deployment/order-service

# Voir les logs de Kafka
kubectl logs deployment/kafka
```

## Commandes essentielles

### Gestion des pods

```bash
# Lister tous les pods
kubectl get pods

# Obtenir des informations détaillées sur un pod
kubectl describe pod <pod-name>

# Afficher les logs d'un pod
kubectl logs <pod-name>

# Suivre les logs en temps réel
kubectl logs -f <pod-name>

# Exécuter une commande dans un pod
kubectl exec -it <pod-name> -- bash
```

### Gestion des services

```bash
# Lister tous les services
kubectl get services

# Obtenir les détails d'un service
kubectl describe service <service-name>

# Port-forward pour accéder à un service
kubectl port-forward service/<service-name> <local-port>:<service-port>
```

### Gestion des déploiements

```bash
# Lister tous les déploiements
kubectl get deployments

# Mettre à l'échelle un déploiement
kubectl scale deployment/<deployment-name> --replicas=3

# Redémarrer un déploiement
kubectl rollout restart deployment/<deployment-name>

# Voir le statut d'un déploiement
kubectl rollout status deployment/<deployment-name>
```

### Nettoyage

```bash
# Supprimer un déploiement
kubectl delete deployment <deployment-name>

# Supprimer un service
kubectl delete service <service-name>

# Supprimer tous les objets définis dans un fichier
kubectl delete -f k8s/kafka-deployment.yml

# Supprimer tous les objets définis dans le répertoire
kubectl delete -k k8s/
```

## Troubleshooting

### Problèmes courants

#### 1. Pods Kafka en CrashLoopBackOff

**Problème** : Les pods Kafka restent en état CrashLoopBackOff.

**Solution** : Vérifiez les logs et assurez-vous que la configuration des LISTENERS est correcte :

```bash
kubectl logs deployment/kafka

# Si vous voyez des erreurs liées aux LISTENERS, modifiez k8s/kafka-deployment.yml :
# - Ajoutez KAFKA_LISTENERS: "PLAINTEXT://:9092"
```

#### 2. Communication Kafka échoue entre les services

**Problème** : Les événements ne sont pas reçus par le consommateur.

**Solution** : Vérifiez la configuration bootstrap-servers dans les microservices :

```bash
# Vérifiez que l'URL Kafka est correcte
kubectl describe configmap

# Vérifiez les logs du service order
kubectl logs deployment/order-service

# Vérifiez les logs du service user
kubectl logs deployment/user-service
```

#### 3. Services inaccessibles

**Problème** : Impossible d'accéder aux services depuis l'extérieur.

**Solution** : Utilisez port-forward ou configurez un Ingress :

```bash
# Port-forward temporaire
kubectl port-forward service/user-service 8081:8081

# Ou exposez le service via minikube
minikube service user-service
```

#### 4. Problèmes d'image Docker

**Problème** : Le pod ne peut pas trouver l'image.

**Solution** : Assurez-vous que les images sont correctement chargées dans Minikube :

```bash
# Vérifier les images disponibles
minikube ssh -- docker images

# Recharger l'image si nécessaire
minikube image load user-service:0.1.0
```

---

Ce guide est spécifique au MVP (v0.1) de notre application microservices avec Kafka sur Kubernetes. Pour les versions ultérieures du projet (avec base de données, CI/CD, etc.), des guides supplémentaires seront fournis.