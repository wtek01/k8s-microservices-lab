# Mise en place du monitoring avec Prometheus et Grafana (Étape b)

Ce document détaille l'implémentation complète du monitoring avec Prometheus et Grafana pour une architecture microservices déployée sur Kubernetes (Minikube). Il s'agit de l'étape b) du projet de version 0.4.

## Table des matières

1. [Prérequis](#prérequis)
2. [Comprendre Prometheus et Grafana](#comprendre-prometheus-et-grafana)
3. [Modification des applications Spring Boot](#modification-des-applications-spring-boot)
4. [Configuration Kubernetes](#configuration-kubernetes)
5. [Installation de Prometheus et Grafana](#installation-de-prometheus-et-grafana)
6. [Configuration de l'Ingress](#configuration-de-lingress)
7. [Accès aux interfaces](#accès-aux-interfaces)
8. [Configuration de Grafana](#configuration-de-grafana)
9. [Utilisation de Prometheus](#utilisation-de-prometheus)
10. [Résolution des problèmes courants](#résolution-des-problèmes-courants)
11. [Commandes utiles](#commandes-utiles)

## Prérequis

- Un cluster Kubernetes fonctionnel (Minikube)
- kubectl configuré pour communiquer avec le cluster
- Helm installé pour déployer des charts
- Applications Spring Boot avec Spring Boot Actuator

## Comprendre Prometheus et Grafana

### Prometheus

Prometheus est un système de monitoring et d'alerte open-source qui permet de collecter et stocker des métriques sous forme de séries temporelles. Il est particulièrement adapté aux environnements dynamiques comme Kubernetes.

Caractéristiques principales:
- Modèle de données multidimensionnel (métriques avec labels)
- Langage de requête flexible (PromQL)
- Architecture autonome sans dépendances externes
- Collection de métriques par "scraping" HTTP
- Support pour la découverte de services

### Grafana

Grafana est une plateforme de visualisation et d'analyse qui permet de créer des tableaux de bord interactifs à partir de diverses sources de données, dont Prometheus.

Caractéristiques principales:
- Tableaux de bord personnalisables
- Support multi-sources
- Système d'alertes
- Partage et export faciles

## Modification des applications Spring Boot

### 1. Ajouter les dépendances nécessaires

Modifiez le fichier `pom.xml` parent pour ajouter les dépendances Micrometer et Prometheus:

```xml
<dependencies>
   <!-- Dépendances existantes... -->
   
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
</dependencies>
```

### 2. Configurer Actuator dans les fichiers application.yml

Pour chaque service (user-service et order-service), mettez à jour le fichier `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

Cette configuration expose les endpoints Actuator nécessaires et active l'export des métriques au format Prometheus.

## Configuration Kubernetes

### 1. Créer les ServiceMonitors pour Prometheus

Créez un fichier `k8s/service-monitors.yml`:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: user-service-monitor
  namespace: monitoring
  labels:
    release: monitoring
spec:
  selector:
    matchLabels:
      app: user-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: order-service-monitor
  namespace: monitoring
  labels:
    release: monitoring
spec:
  selector:
    matchLabels:
      app: order-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

### 2. Mettre à jour les services Kubernetes

Mettez à jour vos fichiers `k8s/user-service.yml` et `k8s/order-service.yml` pour ajouter un nom au port:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  selector:
    app: user-service
  ports:
    - port: 8081
      targetPort: 8081
      name: http  # Ajout du nom du port
```

### 3. Créer un Ingress pour Prometheus et Grafana

Créez un fichier `k8s/monitoring-ingress.yml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: monitoring-ingress
  namespace: monitoring
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    kubernetes.io/ingress.class: "nginx"
spec:
  rules:
    - host: grafana.microservices.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: monitoring-grafana
                port:
                  number: 80
    - host: prometheus.microservices.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: monitoring-kube-prometheus-prometheus
                port:
                  number: 9090
```

### 4. Mise à jour du fichier hosts

Ajoutez les entrées suivantes à votre fichier hosts (`C:\Windows\System32\drivers\etc\hosts` sur Windows):

```
127.0.0.1 grafana.microservices.local
127.0.0.1 prometheus.microservices.local
```

### 5. Mettre à jour kustomization.yaml

Ajoutez les nouveaux fichiers à votre configuration Kustomize dans `k8s/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - kafka-statefulset.yml
  - postgres-deployment.yml
  - user-service.yml
  - order-service.yml
  - ingress.yml
  - monitoring-ingress.yml
  - service-monitors.yml
```

## Installation de Prometheus et Grafana

### 1. Ajouter le repo Helm pour Prometheus

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

### 2. Créer un namespace pour le monitoring

```bash
kubectl create namespace monitoring
```

### 3. Installer Prometheus et Grafana via Helm

```bash
helm install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring
```

### 4. Vérifier l'installation

```bash
kubectl get pods -n monitoring
```

Vous devriez voir plusieurs pods en cours d'exécution, notamment:
- `monitoring-grafana-xxx`
- `prometheus-monitoring-kube-prometheus-prometheus-0`
- `monitoring-kube-prometheus-operator-xxx`

## Configuration de l'Ingress

### 1. S'assurer que l'addon Ingress de Minikube est activé

```bash
minikube addons list | grep ingress
```

Si l'addon est désactivé, activez-le:

```bash
minikube addons enable ingress
```

### 2. Vérifier que les pods Ingress sont en cours d'exécution

```bash
kubectl get pods -n ingress-nginx
```

Vous devriez voir le pod `ingress-nginx-controller-xxx` en cours d'exécution.

### 3. Démarrer le tunnel Minikube

```bash
minikube tunnel
```

Gardez cette commande en cours d'exécution dans un terminal séparé.

### 4. Appliquer la configuration des ServiceMonitors et de l'Ingress

```bash
kubectl apply -f k8s/service-monitors.yml
kubectl apply -f k8s/monitoring-ingress.yml
```

## Accès aux interfaces

### 1. Prometheus

Accédez à Prometheus via http://prometheus.microservices.local

### 2. Grafana

Accédez à Grafana via http://grafana.microservices.local

Identifiants par défaut:
- Username: admin
- Password: obtenez-le avec la commande:
  ```bash
  kubectl -n monitoring get secret monitoring-grafana -o jsonpath="{.data.admin-password}" | base64 --decode
  ```

## Configuration de Grafana

### 1. Vérifier la source de données Prometheus

Dans Grafana:
1. Cliquez sur l'icône ⚙️ (Configuration) dans le menu latéral
2. Sélectionnez "Data Sources"
3. Vérifiez que Prometheus est déjà configuré

Si ce n'est pas le cas, ajoutez-le:
1. Cliquez sur "Add data source"
2. Sélectionnez "Prometheus"
3. URL: http://monitoring-kube-prometheus-prometheus:9090
4. Cliquez sur "Save & Test"

### 2. Importer des tableaux de bord prédéfinis

1. Dans le menu latéral, cliquez sur "+" puis "Import"
2. Importez des tableaux de bord utiles:
    - Spring Boot: ID 11378 ou 6756
    - JVM (Micrometer): ID 4701
    - Kubernetes Cluster: ID 11802
    - Node Exporter: ID 12740

### 3. Créer un tableau de bord personnalisé

1. Cliquez sur "+ New Dashboard"
2. Cliquez sur "Add a new panel"
3. Dans l'éditeur de requête:
    - Source de données: Prometheus
    - Entrez une requête PromQL, par exemple:
        - `http_server_requests_seconds_count{service="user-service"}` (nombre de requêtes HTTP)
        - `rate(http_server_requests_seconds_sum{service="user-service"}[5m]) / rate(http_server_requests_seconds_count{service="user-service"}[5m])` (temps de réponse moyen)
4. Configurez les options d'affichage
5. Cliquez sur "Apply"
6. Ajoutez d'autres panels selon vos besoins

## Utilisation de Prometheus

### 1. Explorer les métriques disponibles

Dans l'interface de Prometheus:
1. Cliquez sur "Graph"
2. Utilisez le menu déroulant pour explorer les métriques disponibles
3. Exemple de requêtes:
    - `up` - Montre l'état de toutes les cibles
    - `jvm_memory_used_bytes` - Utilisation mémoire JVM
    - `http_server_requests_seconds_count` - Nombre de requêtes HTTP

### 2. Vérifier les cibles (targets) configurées

1. Cliquez sur "Status" puis "Targets"
2. Vérifiez que vos services (user-service et order-service) sont présents et à l'état "Up"

## Résolution des problèmes courants

### Problème: Services non visibles dans Prometheus

1. Vérifiez les ServiceMonitors:
   ```bash
   kubectl get servicemonitor -n monitoring
   ```
2. Vérifiez que les labels dans les ServiceMonitors correspondent aux labels des services
3. Vérifiez que le port dans le ServiceMonitor correspond au port nommé dans le Service

### Problème: Impossible d'accéder aux interfaces via l'Ingress

1. Vérifiez que l'addon Ingress est activé:
   ```bash
   minikube addons list | grep ingress
   ```
2. Vérifiez que les pods Ingress sont en cours d'exécution:
   ```bash
   kubectl get pods -n ingress-nginx
   ```
3. Assurez-vous que le tunnel Minikube est actif
4. Vérifiez les entrées dans le fichier hosts
5. Testez l'accès direct via port-forwarding:
   ```bash
   kubectl port-forward svc/monitoring-grafana -n monitoring 3000:80
   kubectl port-forward svc/monitoring-kube-prometheus-prometheus -n monitoring 9090:9090
   ```

## Commandes utiles

### Gestion de Minikube
```bash
# Démarrer Minikube
minikube start

# Vérifier le statut
minikube status

# Activer l'addon Ingress
minikube addons enable ingress

# Démarrer le tunnel
minikube tunnel
```

### Gestion de Helm
```bash
# Ajouter le repo Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Mettre à jour les repos
helm repo update

# Installer le chart kube-prometheus-stack
helm install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring

# Lister les releases Helm
helm list -n monitoring
```

### Gestion de Kubernetes
```bash
# Obtenir les pods dans l'espace de noms monitoring
kubectl get pods -n monitoring

# Obtenir les services dans l'espace de noms monitoring
kubectl get svc -n monitoring

# Obtenir les ServiceMonitors
kubectl get servicemonitor -n monitoring

# Obtenir les Ingress
kubectl get ingress --all-namespaces

# Obtenir le mot de passe Grafana
kubectl -n monitoring get secret monitoring-grafana -o jsonpath="{.data.admin-password}" | base64 --decode

# Accès direct via port-forwarding
kubectl port-forward svc/monitoring-grafana -n monitoring 3000:80
kubectl port-forward svc/monitoring-kube-prometheus-prometheus -n monitoring 9090:9090
```

### Métriques importantes à surveiller

1. **Santé de l'application**:
    - `up{job="user-service"}` ou `up{job="order-service"}`

2. **Performance des requêtes HTTP**:
    - Temps de réponse moyen: `rate(http_server_requests_seconds_sum{service="user-service"}[5m]) / rate(http_server_requests_seconds_count{service="user-service"}[5m])`
    - Taux d'erreurs: `rate(http_server_requests_seconds_count{service="user-service",status="5xx"}[5m])`

3. **Utilisation JVM**:
    - Utilisation mémoire heap: `jvm_memory_used_bytes{service="user-service",area="heap"}`
    - Temps passé en garbage collection: `rate(jvm_gc_pause_seconds_sum[5m])`

4. **Métriques Kafka** (si exposées):
    - `kafka_consumer_fetch_manager_records_consumed_total`

5. **Métriques Kubernetes**:
    - Utilisation CPU: `container_cpu_usage_seconds_total`
    - Utilisation mémoire: `container_memory_usage_bytes`

---

En suivant ces étapes, vous avez mis en place un système de monitoring complet avec Prometheus et Grafana pour surveiller vos microservices Spring Boot déployés sur Kubernetes. Ce monitoring vous permet de visualiser et d'analyser les performances, l'utilisation des ressources et l'état de santé de vos applications.