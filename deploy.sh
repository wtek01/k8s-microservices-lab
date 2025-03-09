#!/bin/bash
# deploy.sh - Script pour construire et déployer la version 0.2 de l'application

# Définir les variables
PROJECT_ROOT=$(pwd)
VERSION="0.2.0"

echo "=== Démarrage du déploiement de la version $VERSION ==="

# Construire les images Docker
echo "Construction des images Docker..."

cd "$PROJECT_ROOT/user-service"
./mvnw clean package -DskipTests
docker build -t user-service:$VERSION .

cd "$PROJECT_ROOT/order-service"
./mvnw clean package -DskipTests
docker build -t order-service:$VERSION .

# Pour un déploiement local avec kubectl (si vous utilisez minikube)
if command -v minikube &> /dev/null; then
    echo "Utilisation de minikube pour le déploiement..."
    eval $(minikube docker-env)

    # Reconstruire les images dans le contexte docker de minikube
    cd "$PROJECT_ROOT/user-service"
    docker build -t user-service:$VERSION .

    cd "$PROJECT_ROOT/order-service"
    docker build -t order-service:$VERSION .
fi

# Déployer les composants Kubernetes
echo "Déploiement sur Kubernetes..."
cd "$PROJECT_ROOT"

echo "Déploiement de PostgreSQL..."
kubectl apply -f postgres-deployment.yml

echo "Attente de 30 secondes pour l'initialisation de PostgreSQL..."
sleep 30

echo "Déploiement de Kafka (si pas déjà déployé)..."
kubectl apply -f kafka-statefulset.yml

echo "Attente de 30 secondes pour l'initialisation de Kafka..."
sleep 30

echo "Déploiement des microservices..."
kubectl apply -f user-service.yml
kubectl apply -f order-service.yml

echo "=== Déploiement terminé ==="
echo "Pour vérifier l'état des pods:"
echo "kubectl get pods"

echo "Pour accéder aux services (si vous utilisez minikube):"
echo "minikube service user-service --url"
echo "minikube service order-service --url"