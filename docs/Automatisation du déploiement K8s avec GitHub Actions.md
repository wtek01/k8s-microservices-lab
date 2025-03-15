# Guide complet : Déploiement automatique sur Kubernetes avec GitHub Actions

Ce guide détaille la mise en place d'un pipeline CI/CD pour le déploiement automatique d'une application Spring Boot avec Kafka sur Kubernetes, en utilisant GitHub Actions et un runner auto-hébergé.

## Table des matières
1. [Architecture globale](#architecture-globale)
2. [Prérequis et installation](#prérequis-et-installation)
3. [Configuration de Minikube](#configuration-de-minikube)
4. [Configuration du GitHub Actions Runner](#configuration-du-github-actions-runner)
5. [Configuration de Docker Hub](#configuration-de-docker-hub)
6. [Configuration du kubeconfig](#configuration-du-kubeconfig)
7. [Structure du projet](#structure-du-projet)
8. [Workflows GitHub Actions](#workflows-github-actions)
9. [Déploiement initial](#déploiement-initial)
10. [Test de l'application](#test-de-l-application)
11. [Troubleshooting](#troubleshooting)
12. [Améliorations possibles](#améliorations-possibles)

## Architecture globale

Le système mis en place comprend :
- **2 microservices Spring Boot** : `user-service` et `order-service`
- **Communication asynchrone** via Kafka
- **Base de données** PostgreSQL
- **Environnement de déploiement** : Kubernetes (Minikube)
- **CI/CD** : GitHub Actions avec runner auto-hébergé

**Architecture des composants :**
```
                   ┌─────────────────┐
                   │                 │
                   │    GitHub       │
                   │   Repository    │
                   │                 │
                   └────────┬────────┘
                           │
                           ▼
                   ┌─────────────────┐
                   │  GitHub Actions │
                   │ Self-hosted     │
                   │     Runner      │
                   └────────┬────────┘
                           │
                           ▼
            ┌──────────────────────────────┐
            │                              │
            │      Minikube Cluster        │
            │                              │
┌───────────┼───────────┐     ┌────────────┼───────────┐
│           │           │     │            │           │
│  ┌────────▼─────────┐│     │┌───────────▼──────────┐│
│  │                  ││     ││                      ││
│  │   user-service   ││     ││   order-service      ││
│  │                  ││     ││                      ││
│  └────────┬─────────┘│     │└───────────┬──────────┘│
│           │           │     │            │           │
│           │           │     │            │           │
│  ┌────────▼─────────┐│     │┌───────────▼──────────┐│
│  │                  ││     ││                      ││
│  │     Kafka        │◄─────┼┤      Kafka           ││
│  │    Consumer      ││     ││     Producer         ││
│  └────────┬─────────┘│     │└────────────────────┬─┘│
│           │           │     │                     │  │
└───────────┼───────────┘     └─────────────────────┼──┘
            │                                       │
            │                                       │
┌───────────▼───────────┐     ┌─────────────────────▼──┐
│                       │     │                        │
│      PostgreSQL       │     │        Kafka           │
│      Database         │     │        Broker          │
│                       │     │                        │
└───────────────────────┘     └────────────────────────┘
```

## Prérequis et installation

### Outils nécessaires

1. **Docker** - Pour la conteneurisation des applications
   ```bash
   # Windows (avec Chocolatey)
   choco install docker-desktop
   
   # Vérifier l'installation
   docker --version
   ```

2. **Minikube** - Pour exécuter Kubernetes localement
   ```bash
   # Windows (avec Chocolatey)
   choco install minikube
   
   # Vérifier l'installation
   minikube version
   ```

3. **kubectl** - Pour interagir avec le cluster Kubernetes
   ```bash
   # Windows (avec Chocolatey)
   choco install kubernetes-cli
   
   # Vérifier l'installation
   kubectl version --client
   ```

4. **JDK 17** - Pour compiler l'application Spring Boot
   ```bash
   # Windows (avec Chocolatey)
   choco install openjdk17
   
   # Vérifier l'installation
   java -version
   ```

5. **Maven** - Pour construire l'application
   ```bash
   # Windows (avec Chocolatey)
   choco install maven
   
   # Vérifier l'installation
   mvn -version
   ```

6. **Git** - Pour la gestion du code source
   ```bash
   # Windows (avec Chocolatey)
   choco install git
   
   # Vérifier l'installation
   git --version
   ```

## Configuration de Minikube

1. **Démarrer Minikube** avec suffisamment de ressources :
   ```bash
   minikube start --cpus 4 --memory 8192 --disk-size 20g
   ```

2. **Activer les addons** nécessaires :
   ```bash
   # Tableau de bord Kubernetes
   minikube addons enable dashboard
   
   # Métriques pour le monitoring
   minikube addons enable metrics-server
   ```

3. **Configurer Docker** pour utiliser le daemon Docker de Minikube :
   ```bash
   # Sur Windows PowerShell (executer en tant qu'administrateur)
   minikube docker-env | Invoke-Expression
   ```

4. **Vérifier le cluster** :
   ```bash
   kubectl get nodes
   ```

## Configuration du GitHub Actions Runner

1. **Accéder à la configuration du runner** dans votre dépôt GitHub :
   - Aller dans `Settings` > `Actions` > `Runners`
   - Cliquer sur `New self-hosted runner`

2. **Télécharger et configurer le runner** sur votre machine Windows :
   ```powershell
   # Créer un dossier pour le runner
   mkdir C:\actions-runner
   cd C:\actions-runner
   
   # Télécharger le runner
   Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.322.0/actions-runner-win-x64-2.322.0.zip -OutFile actions-runner-win-x64-2.322.0.zip
   
   # Extraire le contenu
   Add-Type -AssemblyName System.IO.Compression.FileSystem ; [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/actions-runner-win-x64-2.322.0.zip", "$PWD")
   
   # Configurer le runner
   ./config.cmd --url https://github.com/VOTRE_NOM_UTILISATEUR/VOTRE_REPO --token VOTRE_TOKEN
   ```

3. **Exécuter le runner** en tant que service :
   ```powershell
   # Installer le runner comme service
   ./svc.ps1 install
   
   # Démarrer le service
   ./svc.ps1 start
   ```

4. **Vérifier l'état du runner** dans l'interface GitHub (Settings > Actions > Runners)

## Configuration de Docker Hub

1. **Créer un compte Docker Hub** si vous n'en avez pas déjà un

2. **Créer un token d'accès** :
   - Se connecter à [Docker Hub](https://hub.docker.com)
   - Aller dans `Account Settings` > `Security`
   - Cliquer sur `New Access Token`
   - Nommer le token et le générer
   - Copier le token généré (vous ne pourrez plus le voir après)

3. **Ajouter les secrets Docker Hub à GitHub** :
   - Dans votre dépôt GitHub, aller dans `Settings` > `Secrets and variables` > `Actions`
   - Ajouter deux nouveaux secrets :
      - `DOCKERHUB_USERNAME` : votre nom d'utilisateur Docker Hub
      - `DOCKERHUB_TOKEN` : le token généré précédemment

## Configuration du kubeconfig

1. **Obtenir le fichier kubeconfig** de Minikube :
   ```bash
   cat ~/.kube/config
   ```

2. **Adapter le kubeconfig** pour être utilisé dans GitHub Actions :
   - Assurez-vous que les chemins dans le fichier sont accessibles au runner

3. **Encoder le fichier kubeconfig en base64** :
   ```bash
   cat ~/.kube/config | base64
   ```

4. **Ajouter le kubeconfig encodé à GitHub Secrets** :
   - Dans votre dépôt GitHub, aller dans `Settings` > `Secrets and variables` > `Actions`
   - Ajouter un nouveau secret nommé `KUBE_CONFIG` avec la valeur encodée en base64

## Structure du projet

L'application est organisée en microservices selon la structure suivante :

```
project-root/
│
├── user-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── order-service/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── k8s/
│   ├── kafka-statefulset.yml
│   ├── postgres-deployment.yml
│   ├── user-service.yml
│   ├── order-service.yml
│   └── kustomization.yml
│
├── .github/workflows/
│   ├── main-ci-workflow.yml
│   └── kubernetes-deploy-workflow.yml
│
└── pom.xml (parent)
```

## Workflows GitHub Actions

### Workflow principal (`main-ci-workflow.yml`)

Ce workflow gère la construction, les tests, et la création d'images Docker :

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
    services:
      postgres:
        image: postgres:14-alpine
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: postgres
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Create databases
        run: |
          PGPASSWORD=postgres psql -h localhost -U postgres -c "CREATE DATABASE userdb;"
          PGPASSWORD=postgres psql -h localhost -U postgres -c "CREATE DATABASE orderdb;"

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build and test with Maven
        run: mvn -B clean verify
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/orderdb
          SPRING_DATASOURCE_USERNAME: postgres
          SPRING_DATASOURCE_PASSWORD: postgres
          DB_HOST: localhost
          DB_PORT: 5432
          DB_USER: postgres
          DB_PASSWORD: postgres

      - name: Archive test results
        if: success() || failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: |
            **/target/surefire-reports/
            **/target/failsafe-reports/
          retention-days: 5

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
    runs-on: self-hosted
    permissions:
      contents: write  # Important: donne les permissions d'écriture
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Extract version from pom.xml
        id: get-version
        shell: powershell
        run: |
          $versionLine = Select-String -Path pom.xml -Pattern "<version>(.*?)</version>" | Select-Object -First 1
          $version = $versionLine -replace ".*<version>(.*?)</version>.*", '$1'
          $version = $version -replace "[^0-9\.]", ""
          echo "version=$version" | Out-File -FilePath $env:GITHUB_OUTPUT -Append
          echo "Using version: $version"

      - name: Update Kubernetes manifests
        shell: powershell
        run: |
          # Mettre à jour les manifests avec la nouvelle version d'image
          (Get-Content k8s/user-service.yml) -replace "image: ${{ secrets.DOCKERHUB_USERNAME }}/user-service:.*", "image: ${{ secrets.DOCKERHUB_USERNAME }}/user-service:${{ steps.get-version.outputs.version }}" | Set-Content k8s/user-service.yml
          (Get-Content k8s/order-service.yml) -replace "image: ${{ secrets.DOCKERHUB_USERNAME }}/order-service:.*", "image: ${{ secrets.DOCKERHUB_USERNAME }}/order-service:${{ steps.get-version.outputs.version }}" | Set-Content k8s/order-service.yml

      - name: Commit and push updated manifests
        shell: powershell
        run: |
          git config --local user.email "actions@github.com"
          git config --local user.name "GitHub Actions Bot"
          git add k8s/*.yml
          
          # Tenter de faire un commit et capturer l'erreur éventuelle
          $commitOutput = git commit -m "ci: update k8s manifests with new image versions" 2>&1
          
          # Ignorer l'erreur si c'est "nothing to commit"
          if ($commitOutput -match "nothing to commit" -or $LASTEXITCODE -eq 0) {
              Write-Host "Commit successful or nothing to commit"
              git push origin main --force
          } else {
              Write-Host "Error during commit: $commitOutput"
              exit 1
          }
```

### Workflow de déploiement (`kubernetes-deploy-workflow.yml`)

Ce workflow est responsable du déploiement sur Kubernetes :

```yaml
# .github/workflows/kubernetes-deploy-workflow.yml
name: Deploy to Kubernetes

on:
  push:
    branches: [ main, master ]
    paths:
      - 'k8s/**'  # Se déclenche sur les modifications des fichiers dans le dossier k8s
  workflow_dispatch:  # Pour déclencher manuellement le workflow
  workflow_run:
    workflows: ["Build and Publish"]
    types:
      - completed
    branches: [main, master]

jobs:
  deploy:
    runs-on: self-hosted
    if: ${{ github.event.workflow_run.conclusion == 'success' || github.event_name != 'workflow_run' }}
    permissions:
      contents: read
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3

      - name: Setup kubeconfig
        shell: powershell
        run: |
          if (!(Test-Path -Path "$HOME/.kube")) {
            New-Item -Path "$HOME/.kube" -ItemType Directory
          }
          [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("${{ secrets.KUBE_CONFIG }}")) | Out-File -FilePath "$HOME/.kube/config" -Encoding utf8
          # Définir les permissions appropriées
          $acl = Get-Acl -Path "$HOME/.kube/config"
          $accessRule = New-Object System.Security.AccessControl.FileSystemAccessRule("$env:USERNAME","FullControl","Allow")
          $acl.SetAccessRule($accessRule)
          $acl | Set-Acl -Path "$HOME/.kube/config"

      - name: Verify kubectl configuration
        shell: powershell
        run: |
          kubectl version --client
          Write-Host "Attempting to connect to cluster..."
          try {
            kubectl get nodes
          } catch {
            Write-Host "Failed to connect to cluster"
          }

      - name: Apply Kubernetes manifests
        shell: powershell
        run: |
          Write-Host "Applying Kubernetes configurations..."
          kubectl apply -f k8s/kafka-statefulset.yml
          kubectl apply -f k8s/postgres-deployment.yml
          kubectl apply -f k8s/user-service.yml
          kubectl apply -f k8s/order-service.yml
          # Ou utilisez kustomize
          # kubectl apply -k k8s/

      - name: Force redeploy services
        shell: powershell
        run: |
          Write-Host "Forcing redeployment to ensure new images are used..."
          kubectl rollout restart deployment/user-service
          kubectl rollout restart deployment/order-service

      - name: Verify deployment
        shell: powershell
        run: |
          Write-Host "Verifying deployments..."
          kubectl rollout status deployment/user-service
          kubectl rollout status deployment/order-service
          Write-Host "All deployments successfully rolled out!"
```

## Déploiement initial

Pour le déploiement initial de l'infrastructure, suivez ces étapes :

1. **Créer les manifestes Kubernetes de base** dans le dossier `k8s/` :
   - `kafka-statefulset.yml` - Pour Kafka et Zookeeper
   - `postgres-deployment.yml` - Pour la base de données PostgreSQL
   - `user-service.yml` - Pour le service utilisateur
   - `order-service.yml` - Pour le service de commande

2. **Déployer l'infrastructure** :
   ```bash
   kubectl apply -f k8s/kafka-statefulset.yml
   kubectl apply -f k8s/postgres-deployment.yml
   ```

3. **Créer les secrets nécessaires** :
   ```bash
   kubectl create secret generic postgres-credentials \
     --from-literal=username=postgres \
     --from-literal=password=postgres
   ```

4. **Pousser le code vers GitHub** pour déclencher le pipeline CI/CD :
   ```bash
   git add .
   git commit -m "Initial commit"
   git push origin main
   ```

5. **Vérifier l'exécution des workflows** dans l'onglet Actions de votre dépôt GitHub

## Test de l'application

Une fois l'application déployée, vous pouvez la tester comme suit :

1. **Exposer les services** pour y accéder depuis votre machine locale :
   ```bash
   # Dans un terminal, exposer le service user-service
   kubectl port-forward svc/user-service 8091:8081

   # Dans un autre terminal, exposer le service order-service
   kubectl port-forward svc/order-service 8092:8082
   ```

2. **Tester le service utilisateur** :
   ```bash
   # Créer un utilisateur
   curl -X POST http://localhost:8091/users -H "Content-Type: application/json" -d '{
     "name": "John Doe",
     "email": "john.doe@example.com"
   }'

   # Récupérer la liste des utilisateurs
   curl http://localhost:8091/users
   ```

3. **Tester le service de commande** :
   ```bash
   # Créer une commande (utilisez l'ID utilisateur obtenu précédemment)
   curl -X POST http://localhost:8092/orders -H "Content-Type: application/json" -d '{
     "userId": "ID_UTILISATEUR_OBTENU",
     "productId": "prod-001",
     "amount": 99.99
   }'

   # Récupérer la liste des commandes
   curl http://localhost:8092/orders
   ```

4. **Vérifier l'intégration Kafka** :
   ```bash
   # Récupérer les détails de l'utilisateur pour voir si la commande a été ajoutée
   curl http://localhost:8091/users/ID_UTILISATEUR_OBTENU
   ```

5. **Vérifier les logs** pour confirmer que tout fonctionne correctement :
   ```bash
   # Vérifier les logs du service de commandes
   kubectl logs deployment/order-service

   # Vérifier les logs du service utilisateur
   kubectl logs deployment/user-service
   ```

## Troubleshooting

### Problèmes courants et solutions

1. **Erreur de configuration kubeconfig** :
   - **Symptôme** : `Unable to connect to the server: dial tcp [::1]:8080: connectex: No connection could be made`
   - **Solution** : Vérifier que le fichier kubeconfig est correctement configuré et pointe vers la bonne adresse IP du cluster

2. **Erreur de syntaxe PowerShell** :
   - **Symptôme** : `The token '||' is not a valid statement separator in this version`
   - **Solution** : Remplacer les opérateurs bash comme `||` par des structures PowerShell appropriées (try-catch ou if-else)

3. **Erreur d'accès au dépôt Git** :
   - **Symptôme** : `Permission to repository denied to github-actions[bot]`
   - **Solution** : Ajouter `permissions: contents: write` au job concerné

4. **Les images ne sont pas mises à jour** :
   - **Symptôme** : Les pods continuent d'utiliser les anciennes images
   - **Solution** : Ajouter une étape de redéploiement forcé avec `kubectl rollout restart deployment/...`

5. **Échec des sondes de disponibilité** :
   - **Symptôme** : `Readiness probe failed: Get "http://10.244.0.27:8081/actuator/health": context deadline exceeded`
   - **Solution** : Vérifier que les services exposent correctement leurs endpoints de santé et ajuster les délais des sondes

### Commandes de diagnostic

```bash
# Vérifier l'état des pods
kubectl get pods

# Afficher les détails d'un pod
kubectl describe pod <nom-du-pod>

# Afficher les logs d'un pod
kubectl logs <nom-du-pod>

# Vérifier les services
kubectl get services

# Vérifier les déploiements
kubectl get deployments

# Vérifier les événements du cluster
kubectl get events --sort-by=.metadata.creationTimestamp
```

## Améliorations possibles

Pour améliorer davantage votre pipeline CI/CD et votre architecture, envisagez :

1. **Ingress Controller** pour exposer les services :
   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: Ingress
   metadata:
     name: api-ingress
     annotations:
       nginx.ingress.kubernetes.io/rewrite-target: /
   spec:
     rules:
     - host: api.local
       http:
         paths:
         - path: /users
           pathType: Prefix
           backend:
             service:
               name: user-service
               port:
                 number: 8081
         - path: /orders
           pathType: Prefix
           backend:
             service:
               name: order-service
               port:
                 number: 8082
   ```

2. **Monitoring avec Prometheus et Grafana** :
   - Ajouter des annotations pour Prometheus dans vos déploiements
   - Configurer Grafana pour visualiser les métriques

3. **Tests d'intégration post-déploiement** dans le workflow CI/CD :
   ```yaml
   - name: Run integration tests
     run: |
       # Attendre que les services soient prêts
       sleep 30
       # Exécuter les tests d'intégration
       curl -f http://localhost:8091/actuator/health
       curl -f http://localhost:8092/actuator/health
   ```

4. **Stratégie de déploiement bleu/vert** pour des mises à jour sans interruption :
   ```yaml
   strategy:
     type: RollingUpdate
     rollingUpdate:
       maxUnavailable: 0
       maxSurge: 1
   ```

5. **Mettre en place Vault** pour une gestion plus sécurisée des secrets

Ces améliorations vous permettront de construire une plateforme de microservices plus robuste, évolutive et facile à gérer.