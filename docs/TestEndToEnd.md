Compris ! Adaptons le use case pour Windows 11. Voici une version modifiée qui utilise PowerShell et des commandes Windows pour tester votre environnement et configurer le monitoring.

# Use Case : Test end-to-end et monitoring d'une application microservices (Windows 11)

## Partie 1 : Test fonctionnel end-to-end

### Étape 1 : Génération de charge simulée

Commençons par créer un script PowerShell pour générer des utilisateurs et des commandes :

```powershell
# Créer un script PowerShell pour générer de la charge
$scriptContent = @'
# Nombre d'utilisateurs à créer
$numUsers = 10
# Nombre de commandes par utilisateur
$ordersPerUser = 5
# URL de base (avec Ingress)
$baseUrl = "http://api.microservices.local"

# Création des utilisateurs
Write-Host "Création de $numUsers utilisateurs..."
for ($i = 1; $i -le $numUsers; $i++) {
    Write-Host "Création de l'utilisateur $i..."
    $userJson = @{
        name = "Utilisateur Test $i"
        email = "user$i@example.com"
    } | ConvertTo-Json
    
    $userResponse = Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -ContentType "application/json" -Body $userJson -ErrorAction SilentlyContinue
    
    if ($userResponse -and $userResponse.id) {
        $userId = $userResponse.id
        Write-Host "Utilisateur créé avec ID: $userId"
        
        # Création des commandes pour cet utilisateur
        Write-Host "Création de $ordersPerUser commandes pour l'utilisateur $i..."
        for ($j = 1; $j -le $ordersPerUser; $j++) {
            # Montant aléatoire entre 10 et 200
            $amount = Get-Random -Minimum 10 -Maximum 200
            $productId = "PROD-" + (Get-Random -Minimum 1 -Maximum 100)
            
            Write-Host "Création de la commande $j avec montant $amount..."
            $orderJson = @{
                userId = $userId
                productId = $productId
                amount = $amount
            } | ConvertTo-Json
            
            $orderResponse = Invoke-RestMethod -Uri "$baseUrl/orders" -Method Post -ContentType "application/json" -Body $orderJson -ErrorAction SilentlyContinue
            
            if ($orderResponse -and $orderResponse.id) {
                Write-Host "Commande créée avec ID: $($orderResponse.id)"
            } else {
                Write-Host "Échec de la création de la commande" -ForegroundColor Red
            }
            
            # Pause aléatoire entre 0.5 et 2 secondes
            Start-Sleep -Milliseconds (Get-Random -Minimum 500 -Maximum 2000)
        }
    } else {
        Write-Host "Échec de la création de l'utilisateur" -ForegroundColor Red
    }
    
    # Pause aléatoire entre 1 et 3 secondes
    Start-Sleep -Seconds (Get-Random -Minimum 1 -Maximum 3)
}

Write-Host "Test terminé!" -ForegroundColor Green
'@

# Enregistrer le script dans un fichier
$scriptContent | Out-File -FilePath ".\test-load.ps1" -Encoding utf8

# Exécuter le script
Write-Host "Exécution du script de test..."
& .\test-load.ps1
```

Copiez ce code dans PowerShell pour créer et exécuter votre script de test.

### Étape 2 : Vérification de l'intégrité des données

```powershell
# Pour chaque utilisateur créé, vérifier ses commandes associées
for ($i = 1; $i -le 10; $i++) {
    $userEmail = "user$i@example.com"
    Write-Host "Vérification de l'utilisateur avec email $userEmail..." -ForegroundColor Cyan
    
    # Obtenir tous les utilisateurs
    $users = Invoke-RestMethod -Uri "http://api.microservices.local/users" -Method Get -ErrorAction SilentlyContinue
    
    # Trouver l'utilisateur spécifique
    $user = $users | Where-Object { $_.email -eq $userEmail }
    
    if ($user) {
        $userId = $user.id
        Write-Host "ID utilisateur trouvé: $userId"
        
        # Vérifier les détails de l'utilisateur spécifique
        $userDetails = Invoke-RestMethod -Uri "http://api.microservices.local/users/$userId" -Method Get -ErrorAction SilentlyContinue
        
        if ($userDetails -and $userDetails.orderIds) {
            $orderCount = $userDetails.orderIds.Count
            Write-Host "L'utilisateur a $orderCount commandes: $($userDetails.orderIds -join ', ')"
        } else {
            Write-Host "Aucune commande trouvée pour cet utilisateur" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Utilisateur avec email $userEmail non trouvé" -ForegroundColor Red
    }
    
    Write-Host "------------------------"
}
```

## Partie 2 : Configuration du monitoring avec Prometheus

### Étape 1 : Explorer les métriques disponibles dans Prometheus

Accédez à Prometheus via votre navigateur :
- http://prometheus.microservices.local

Si l'accès via Ingress ne fonctionne pas, utilisez le port-forward :

```powershell
# Ouvrir un nouveau terminal PowerShell
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
```

Puis accédez à http://localhost:9090 dans votre navigateur.

Dans l'interface Prometheus, explorez les métriques :

1. Cliquez sur "Graph"
2. Dans le champ de recherche, explorez ces métriques :
    - `http_server_requests_seconds_count` (nombre de requêtes HTTP)
    - `http_server_requests_seconds_sum` (temps total des requêtes)
    - `process_cpu_usage` (utilisation CPU)
    - `jvm_memory_used_bytes` (utilisation mémoire)
    - `kafka_consumer_fetch_manager_records_consumed_total` (messages Kafka consommés)

### Étape 2 : Tester des requêtes PromQL

Dans l'interface Prometheus, testez ces requêtes :

1. Taux de requêtes HTTP par service :
```
sum by (job) (rate(http_server_requests_seconds_count{job=~"user-service|order-service"}[5m]))
```

2. Latence moyenne des requêtes HTTP :
```
sum by (job) (rate(http_server_requests_seconds_sum{job=~"user-service|order-service"}[5m])) / 
sum by (job) (rate(http_server_requests_seconds_count{job=~"user-service|order-service"}[5m]))
```

3. Utilisation CPU :
```
process_cpu_usage{job=~"user-service|order-service"}
```

4. Consommation mémoire Java :
```
sum by (job) (jvm_memory_used_bytes{area="heap"})
```

## Partie 3 : Configuration des tableaux de bord Grafana

Accédez à Grafana via http://grafana.microservices.local ou le port-forward :

```powershell
# Ouvrir un nouveau terminal PowerShell
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
```

Puis accédez à http://localhost:3000 dans votre navigateur.

Utilisez les identifiants par défaut (admin/admin) pour vous connecter.

### Étape 1 : Configurer la source de données Prometheus

1. Allez dans "Configuration" (icône d'engrenage) > "Data sources"
2. Cliquez sur "Add data source"
3. Sélectionnez "Prometheus"
4. Dans le champ URL, entrez `http://monitoring-kube-prometheus-prometheus.monitoring.svc.cluster.local:9090`
5. Cliquez sur "Save & Test"

### Étape 2 : Importer un tableau de bord prédéfini pour Spring Boot

1. Allez dans "+" > "Import"
2. Entrez le code `12900` dans le champ "Import via grafana.com"
3. Cliquez sur "Load"
4. Sélectionnez votre source de données Prometheus
5. Cliquez sur "Import"

Vous aurez un tableau de bord Spring Boot préconfiguré.

### Étape 3 : Créer un tableau de bord personnalisé

1. Cliquez sur "+" > "Create Dashboard"
2. Cliquez sur "Add new panel"
3. Configurez le premier panneau :
    - Titre : "Trafic API par service"
    - PromQL : `sum by (job, uri) (rate(http_server_requests_seconds_count[1m]))`
    - Type de visualisation : "Time series"
4. Cliquez sur "Apply"

5. Ajoutez un second panneau :
    - Titre : "Santé des services"
    - PromQL : `up{job=~"user-service|order-service"}`
    - Type de visualisation : "Stat"
    - Threshold : 1 = "green", 0 = "red"
6. Cliquez sur "Apply"

7. Enregistrez le tableau de bord sous "Microservices Overview"

## Partie 4 : Test de résilience

### Étape 1 : Simuler une panne du service

```powershell
# Simuler une panne du service de commande
kubectl scale deployment order-service --replicas=0

# Observer l'état des pods
kubectl get pods -l app=order-service --watch

# Dans un autre terminal PowerShell, générer du trafic pendant la panne
& .\test-load.ps1

# Observer les métriques dans Grafana et Prometheus
# Après quelques minutes, restaurer le service
kubectl scale deployment order-service --replicas=1
```

### Étape 2 : Observer la récupération

Observez dans Grafana :
1. Le retour du service à la normale
2. La récupération des messages Kafka en attente

## Partie 5 : Configuration des alertes

### Étape 1 : Créer une règle d'alerte dans Grafana

1. Éditez un panneau (par exemple celui de latence)
2. Cliquez sur "Alert" dans le panneau de gauche
3. Cliquez sur "Create alert rule from this panel"
4. Configurez l'alerte :
    - Name : "Latence élevée"
    - Condition : when "B" is above 0.5 for 1m
    - Notifications : Configurez selon vos préférences
5. Cliquez sur "Save" pour créer l'alerte

## Partie 6 : Capture d'écran des tableaux de bord

Prenez des captures d'écran de vos tableaux de bord Grafana pour la documentation :

1. Vue d'ensemble des services
2. Détails du service utilisateur
3. Détails du service de commande
4. Statistiques Kafka

## Partie 7 : Création d'un rapport de performance

Créez un document Word ou Markdown pour résumer les performances :

```powershell
# Obtenir des informations système pour le rapport
$report = @"
# Rapport de performance des microservices

## Environnement
- Minikube version: $(minikube version)
- Kubernetes version: $(kubectl version --short)
- OS: Windows 11

## Services déployés
$(kubectl get pods -o wide)

## Performances observées
- Débit moyen: XX requêtes/seconde
- Latence moyenne: XX ms
- Utilisation mémoire: XX MB
- Utilisation CPU: XX%

## Recommandations
1. Augmenter les ressources pour le service X
2. Optimiser les requêtes pour l'endpoint Y
3. Ajouter plus de réplicas pour améliorer la disponibilité
"@

# Enregistrer le rapport
$report | Out-File -FilePath ".\performance-report.md" -Encoding utf8
```

## Conclusion

Ce use case vous permet de tester en profondeur votre environnement microservices sur Windows 11 en:

1. Générant du trafic réaliste avec PowerShell
2. Configurant des tableaux de bord Grafana pour la visualisation
3. Utilisant Prometheus pour collecter et interroger les métriques
4. Testant la résilience de votre système
5. Configurant des alertes pour être prévenu des problèmes

Avec ces outils en place, vous pourrez surveiller efficacement l'état de santé et les performances de votre application, et réagir rapidement aux problèmes potentiels.