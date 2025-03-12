# Guide complet CI/CD pour microservices Spring Boot avec Kafka sur Kubernetes

Ce document résume notre discussion complète sur la mise en place d'un pipeline CI/CD pour votre projet de microservices Spring Boot communiquant via Kafka et déployés sur Kubernetes.

## 1. Configuration du pipeline CI/CD (Version 0.3)

### 1.1. Structure du workflow GitHub Actions

Nous avons créé deux workflows distincts :

#### 1.1.1. Workflow principal (`main-ci-workflow.yml`)

Ce workflow est déclenché lors des push sur les branches principales (main ou master) et comprend trois jobs principaux :

##### a) Build et Test (`build-and-test`)
- Configure Java 17
- Exécute `mvn clean verify` pour construire et tester les microservices
- Archive les résultats des tests
- Met en cache les dépendances Maven

##### b) Construction et publication des images Docker (`build-and-push-images`)
- Utilise une matrice pour traiter les deux services en parallèle
- Se connecte à Docker Hub avec vos identifiants
- Extrait le numéro de version depuis votre pom.xml parent
- Construit les images Docker
- Pousse les images vers Docker Hub avec le tag de version approprié

##### c) Mise à jour des manifestes Kubernetes (`update-k8s-manifests`)
- Met à jour les manifestes Kubernetes pour utiliser les nouvelles images
- Commit et pousse ces changements sur une branche dédiée `k8s-manifests`

```yaml
# .github/workflows/main-ci-workflow.yml
name: Build and Publish

on:
  push:
    branches: [ main, master ]
    paths-ignore:
      - '**.md'
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:  # Pour déclencher manuellement le workflow

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build and test with Maven
        run: mvn -B clean verify

      - name: Archive test results
        if: success() || failure()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: |
            **/target/surefire-reports/
            **/target/failsafe-reports/
          retention-days: 5

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

  build-and-push-images:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.event_name == 'push' || github.event_name == 'workflow_dispatch'
    strategy:
      matrix:
        service: [user-service, order-service]
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Extract version from pom.xml
        id: get-version
        run: |
          VERSION=$(grep -m1 "<version>" pom.xml | sed 's/[^0-9\.]//g')
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Using version: $VERSION"

      - name: Docker metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ secrets.DOCKERHUB_USERNAME }}/${{ matrix.service }}
          tags: |
            type=semver,pattern={{version}},value=${{ steps.get-version.outputs.version }}
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        uses: docker/build-push-action@v3
        with:
          context: .
          file: ${{ matrix.service }}/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=registry,ref=${{ secrets.DOCKERHUB_USERNAME }}/${{ matrix.service }}:buildcache
          cache-to: type=registry,ref=${{ secrets.DOCKERHUB_USERNAME }}/${{ matrix.service }}:buildcache,mode=max

  update-k8s-manifests:
    needs: build-and-push-images
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Extract version from pom.xml
        id: get-version
        run: |
          VERSION=$(grep -m1 "<version>" pom.xml | sed 's/[^0-9\.]//g')
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Using version: $VERSION"

      - name: Update Kubernetes manifests
        run: |
          # Mettre à jour les manifests avec la nouvelle version d'image
          sed -i "s|image: user-service:.*|image: ${{ secrets.DOCKERHUB_USERNAME }}/user-service:${{ steps.get-version.outputs.version }}|g" k8s/user-service.yml
          sed -i "s|image: order-service:.*|image: ${{ secrets.DOCKERHUB_USERNAME }}/order-service:${{ steps.get-version.outputs.version }}|g" k8s/order-service.yml

      - name: Commit and push updated manifests
        uses: stefanzweifel/git-auto-commit-action@v4
        with:
          commit_message: "ci: update k8s manifests with new image versions"
          file_pattern: "k8s/*.yml"
          branch: k8s-manifests
          create_branch: true
```

#### 1.1.2. Workflow de déploiement (`kubernetes-deploy-workflow.yml`)

Ce workflow est déclenché lorsque des changements sont poussés sur la branche `k8s-manifests` :

```yaml
# .github/workflows/kubernetes-deploy-workflow.yml
name: Deploy to Kubernetes

on:
  push:
    branches: [ k8s-manifests ]
  workflow_dispatch:  # Pour déclencher manuellement le workflow

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up kubeconfig
        uses: azure/k8s-set-context@v2
        with:
          kubeconfig: ${{ secrets.KUBE_CONFIG }}
          # OU utiliser ces champs pour se connecter à un cluster EKS/AKS/GKE
          # method: service-account
          # k8s-url: ${{ secrets.K8S_URL }}
          # k8s-secret: ${{ secrets.K8S_SECRET }}

      - name: Apply Kubernetes manifests
        run: |
          echo "Applying Kubernetes configurations..."
          kubectl apply -f k8s/kafka-statefulset.yml
          kubectl apply -f k8s/postgres-deployment.yml
          kubectl apply -f k8s/user-service.yml
          kubectl apply -f k8s/order-service.yml
          # Ou utilisez kustomize
          # kubectl apply -k k8s/

      - name: Verify deployment
        run: |
          echo "Verifying deployments..."
          kubectl rollout status deployment/user-service
          kubectl rollout status deployment/order-service
          echo "All deployments successfully rolled out!"
```

### 1.2. Dockerfiles optimisés

#### 1.2.1. Dockerfile pour user-service

```dockerfile
# Étape de construction
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

# Copier le pom parent et ses dépendances
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copier le module user-service
COPY user-service/pom.xml ./user-service/
RUN ./mvnw dependency:go-offline -B -pl user-service

# Copier le code source et construire
COPY user-service/src ./user-service/src/
RUN ./mvnw package -DskipTests -pl user-service

# Étape de déploiement
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

# Copier l'application compilée
COPY --from=build /workspace/app/user-service/target/*.jar /app/app.jar

# Script wait-for-it pour attendre les dépendances
RUN echo '#!/bin/sh\n\
set -e\n\
\n\
host="$1"\n\
port="$2"\n\
shift 2\n\
cmd="$@"\n\
\n\
until nc -z "$host" "$port" || [ "$WAIT_TIMEOUT" = "0" ]; do\n\
  >&2 echo "Service on $host:$port is unavailable - sleeping"\n\
  sleep 1\n\
done\n\
\n\
>&2 echo "Service on $host:$port is up - executing command"\n\
exec $cmd' > /wait-for-it.sh && \
chmod +x /wait-for-it.sh

# Installer netcat pour le script wait-for-it
RUN apk add --no-cache netcat-openbsd

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Exécuter l'application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### 1.2.2. Dockerfile pour order-service

```dockerfile
# Étape de construction
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

# Copier le pom parent et ses dépendances
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copier le module order-service
COPY order-service/pom.xml ./order-service/
RUN ./mvnw dependency:go-offline -B -pl order-service

# Copier le code source et construire
COPY order-service/src ./order-service/src/
RUN ./mvnw package -DskipTests -pl order-service

# Étape de déploiement
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp

# Copier l'application compilée
COPY --from=build /workspace/app/order-service/target/*.jar /app/app.jar

# Script wait-for-it pour attendre les dépendances
RUN echo '#!/bin/sh\n\
set -e\n\
\n\
host="$1"\n\
port="$2"\n\
shift 2\n\
cmd="$@"\n\
\n\
until nc -z "$host" "$port" || [ "$WAIT_TIMEOUT" = "0" ]; do\n\
  >&2 echo "Service on $host:$port is unavailable - sleeping"\n\
  sleep 1\n\
done\n\
\n\
>&2 echo "Service on $host:$port is up - executing command"\n\
exec $cmd' > /wait-for-it.sh && \
chmod +x /wait-for-it.sh

# Installer netcat pour le script wait-for-it
RUN apk add --no-cache netcat-openbsd

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Exécuter l'application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 1.3. Manifestes Kubernetes

#### 1.3.1. Manifeste pour user-service

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
        - name: user-service
          image: yourusername/user-service:0.2.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
              value: "kafka:9092"
            - name: DB_HOST
              value: "postgres"
            - name: DB_PORT
              value: "5432"
            - name: DB_NAME
              value: "userdb"
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: password
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 20
          resources:
            limits:
              memory: "512Mi"
              cpu: "500m"
            requests:
              memory: "256Mi"
              cpu: "250m"
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
    - port: 8081
      targetPort: 8081
```

#### 1.3.2. Manifeste pour order-service

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  labels:
    app: order-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: yourusername/order-service:0.2.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8082
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
              value: "kafka:9092"
            - name: DB_HOST
              value: "postgres"
            - name: DB_PORT
              value: "5432"
            - name: DB_NAME
              value: "orderdb"
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: password
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 20
          resources:
            limits:
              memory: "512Mi"
              cpu: "500m"
            requests:
              memory: "256Mi"
              cpu: "250m"
---
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - port: 8082
      targetPort: 8082
```

## 2. Configuration des secrets GitHub

### 2.1. Secrets requis

Pour que le pipeline CI/CD fonctionne correctement, vous devez configurer les secrets suivants dans votre dépôt GitHub :

1. **DOCKERHUB_USERNAME** : Votre nom d'utilisateur Docker Hub (dans votre cas : "wtek01")
2. **DOCKERHUB_TOKEN** : Un token d'accès personnel Docker Hub (dckr_pat_lSpudo6rchp-yD4Ske8ItPswoZk)
3. **KUBE_CONFIG** : Le contenu de votre fichier kubeconfig encodé en base64

### 2.2. Procédure pour obtenir un token Docker Hub

1. Connectez-vous à votre compte Docker Hub
2. Cliquez sur votre avatar > Personal Access Tokens
3. Cliquez sur "Generate new token"
4. Donnez un nom au token et définissez les autorisations appropriées
5. Copiez immédiatement le token généré car il ne sera plus accessible ultérieurement

### 2.3. Comment configurer les secrets GitHub

1. Accédez à votre dépôt GitHub
2. Cliquez sur "Settings" (Paramètres)
3. Dans le menu latéral gauche, cherchez "Secrets and variables" puis cliquez sur "Actions"
4. Cliquez sur "New repository secret"
5. Ajoutez chaque secret avec son nom et sa valeur correspondante

## 3. Lien entre GitHub Actions et Kubernetes

### 3.1. Comment fonctionne l'interaction

GitHub Actions n'exécute pas vos applications. C'est un service d'automatisation qui :
1. Clone votre code
2. Construit vos applications
3. Crée des images Docker
4. Pousse ces images vers Docker Hub
5. **Se connecte à votre cluster Kubernetes** en utilisant le fichier kubeconfig fourni
6. Applique les fichiers de déploiement YAML à votre cluster

### 3.2. Analogie explicative

- GitHub Actions est comme un constructeur à distance
- Votre cluster Kubernetes est votre maison
- Le kubeconfig est comme une clé et l'adresse de votre maison
- Les manifestes Kubernetes sont les plans pour les meubles

Le constructeur (GitHub Actions) prend les plans (manifestes), fabrique les meubles (images Docker), puis utilise la clé et l'adresse (kubeconfig) pour entrer dans votre maison (cluster) et installer les meubles (déployer les applications).

### 3.3. Accessibilité du cluster

Pour que GitHub Actions puisse déployer sur votre cluster, celui-ci doit être accessible depuis Internet, ce qui signifie que :

1. Si vous utilisez un cluster cloud (EKS, GKE, AKS), il est généralement déjà accessible
2. Si vous utilisez un cluster local, vous devrez le rendre accessible ou utiliser une méthode alternative comme un runner auto-hébergé

## 4. Options pour créer un cluster Kubernetes

### 4.1. Solutions locales

1. **Minikube**
    - Cluster mono-nœud pour le développement local
    - Facile à installer et à utiliser

2. **K3s**
    - Distribution légère de Kubernetes
    - Parfait pour les environnements avec ressources limitées
   ```bash
   curl -sfL https://get.k3s.io | sh -
   ```

3. **Kind (Kubernetes IN Docker)**
    - Exécute des clusters Kubernetes dans des conteneurs Docker
   ```bash
   GO111MODULE="on" go install sigs.k8s.io/kind@latest
   kind create cluster --name mon-cluster
   ```

### 4.2. Solutions avancées

1. **Kubeadm**
    - Outil officiel pour créer des clusters Kubernetes conformes
   ```bash
   apt-get update && apt-get install -y kubelet kubeadm kubectl
   kubeadm init --pod-network-cidr=10.244.0.0/16
   ```

2. **k0s**
    - Distribution tout-en-un sans dépendances
   ```bash
   curl -sSLf https://get.k0s.sh | sudo sh
   sudo k0s install controller --single
   sudo k0s start
   ```

3. **Kubespray**
    - Utilise Ansible pour déployer des clusters
   ```bash
   git clone https://github.com/kubernetes-sigs/kubespray.git
   cd kubespray
   pip install -r requirements.txt
   cp -rfp inventory/sample inventory/mycluster
   ```

### 4.3. Solutions cloud

1. **AWS EKS** (Elastic Kubernetes Service)
2. **Google GKE** (Google Kubernetes Engine)
3. **Azure AKS** (Azure Kubernetes Service)
4. **DigitalOcean Kubernetes**

## 5. Tests et vérification du déploiement

### 5.1. Vérifier l'exécution du workflow

1. Poussez votre code sur GitHub
   ```bash
   git add .
   git commit -m "Ajout du pipeline CI/CD avec GitHub Actions"
   git push origin main
   ```

2. Allez sur votre dépôt GitHub et cliquez sur l'onglet "Actions"
3. Vérifiez que votre workflow s'exécute correctement

### 5.2. Vérifier le déploiement des services

```bash
# Vérifier les pods
kubectl get pods

# Vérifier les services
kubectl get services

# Vérifier les détails des déploiements
kubectl describe deployment user-service
kubectl describe deployment order-service
```

### 5.3. Tester les services déployés

Pour tester vos services déployés, utilisez **kubectl port-forward** pour créer un tunnel entre votre machine locale et le service dans le cluster :

```bash
# Port-forward pour le service utilisateur
kubectl port-forward svc/user-service 8091:8081

# Dans un autre terminal, port-forward pour le service commande
kubectl port-forward svc/order-service 8092:8082
```

Puis testez avec curl :
```bash
# Test du service utilisateur
curl http://localhost:8091/users

# Test du service commande
curl http://localhost:8092/orders
```

**Important** : Avec port-forward, vous n'avez pas besoin de démarrer les services localement. La commande crée un tunnel vers les services qui tournent déjà dans votre cluster Kubernetes.

### 5.4. Test complet de bout en bout

1. Créez un utilisateur :
```bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"Test User","email":"test@example.com"}' http://localhost:8091/users
```

2. Créez une commande en utilisant l'ID d'utilisateur retourné :
```bash
curl -X POST -H "Content-Type: application/json" -d '{"userId":"USER_ID_FROM_PREVIOUS_STEP","productId":"product123","amount":100.50}' http://localhost:8092/orders
```

3. Vérifiez que l'événement Kafka est traité en consultant les logs :
```bash
kubectl logs -f deployment/user-service | grep "Received OrderCreatedEvent"
```

## 6. Résolution des problèmes courants

### 6.1. Problèmes d'accès à Docker Hub

- Vérifiez que les secrets DOCKERHUB_USERNAME et DOCKERHUB_TOKEN sont correctement configurés
- Assurez-vous que le token Docker Hub a les permissions nécessaires (lecture/écriture)

### 6.2. Problèmes de déploiement Kubernetes

- Vérifiez que votre cluster est accessible depuis Internet
- Assurez-vous que le secret KUBE_CONFIG est correctement configuré
- Vérifiez les logs du workflow pour identifier les erreurs spécifiques

### 6.3. Problèmes avec les services déployés

- Vérifiez les logs des pods avec `kubectl logs <nom-du-pod>`
- Vérifiez que les services sont correctement exposés
- Assurez-vous que la configuration des ressources est suffisante

## 7. Alternatives pour le cluster Kubernetes

### 7.1. Runner GitHub Actions auto-hébergé

Si votre cluster n'est pas accessible depuis Internet, vous pouvez héberger votre propre runner GitHub Actions :

```yaml
jobs:
  deploy:
    runs-on: self-hosted  # Utilise votre propre runner
    steps:
      # ...
```

### 7.2. Génération des manifestes sans déploiement

Vous pouvez modifier le workflow pour qu'il génère uniquement les manifestes mis à jour sans tenter de les appliquer :

```yaml
- name: Update Kubernetes manifests
  run: |
    # Mettre à jour les manifests avec la nouvelle version d'image
    sed -i "s|image: user-service:.*|image: ${{ secrets.DOCKERHUB_USERNAME }}/user-service:${{ steps.get-version.outputs.version }}|g" k8s/user-service.yml
    sed -i "s|image: order-service:.*|image: ${{ secrets.DOCKERHUB_USERNAME }}/order-service:${{ steps.get-version.outputs.version }}|g" k8s/order-service.yml
```

## 8. Conclusion

Ce guide complet vous a montré comment configurer un pipeline CI/CD avec GitHub Actions pour vos microservices Spring Boot communiquant via Kafka et déployés sur Kubernetes. Vous avez maintenant les connaissances nécessaires pour automatiser le processus de build, test et déploiement de vos applications, ce qui augmentera votre productivité et la fiabilité de vos déploiements.

N'hésitez pas à adapter ce pipeline à vos besoins spécifiques et à explorer d'autres fonctionnalités avancées comme les déploiements blue/green, les tests automatisés de sécurité, ou l'intégration avec des outils de monitoring.