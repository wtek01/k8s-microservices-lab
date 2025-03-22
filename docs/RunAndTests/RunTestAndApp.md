# Guide de démarrage et test du projet - Environnement Minikube avec Actions Runner

Ce guide vous accompagne étape par étape pour vérifier, démarrer et tester votre application microservices après un déploiement automatique via GitHub Actions Runner auto-hébergé sur Minikube.

## Prérequis

- Minikube installé et configuré
- GitHub Actions Runner auto-hébergé
- `kubectl` configuré pour utiliser le contexte Minikube
- `curl` ou Postman pour tester les API REST
- DockerDesktop installé et démarré

## 1. Vérifier l'état de Minikube

```bash
# Vérifier que Minikube est en cours d'exécution
minikube status

# Si nécessaire, démarrer Minikube
minikube start
```

## 2. Démarrer le tunnel Minikube pour l'Ingress

Cette étape est cruciale pour exposer l'Ingress et y accéder depuis votre machine locale :

```bash
# Ouvrez un terminal dédié et exécutez
minikube tunnel
```

Gardez cette fenêtre ouverte pendant vos tests. Cette commande crée un réseau entre votre système hôte et Minikube, permettant d'accéder aux services LoadBalancer et aux Ingress.

## 3. Démarrer le GitHub Actions Runner

Ouvrez un nouveau terminal et lancez le runner avec :

```bash
# Naviguez vers le répertoire du runner
cd C:\Dev\GitHubActions\actions-runner

# Démarrez le runner
.\run.cmd
```

Gardez cette fenêtre ouverte. Le runner va se mettre en attente de jobs provenant de GitHub.

## 4. Vérifier l'état du déploiement

```bash
# Vérifier tous les pods
kubectl get pods

# Vérifier chaque composant
kubectl get pods -l app=user-service
kubectl get pods -l app=order-service
kubectl get pods -l app=postgres
kubectl get pods -l app=kafka
kubectl get pods -l app=zookeeper
```

Assurez-vous que tous les pods sont à l'état `Running` et prêts (`1/1`, `2/2`, etc.).

## 5. Vérifier les services et l'ingress

```bash
# Vérifier les services
kubectl get services

# Vérifier l'ingress
kubectl get ingress
```

Vérifiez que l'addon Ingress est activé dans Minikube :

```bash
minikube addons enable ingress
```

## 6. Configurer votre fichier hosts local

Avec `minikube tunnel` en cours d'exécution, vous pouvez utiliser `127.0.0.1` comme adresse IP pour accéder à l'Ingress :

```
127.0.0.1 api.microservices.local
127.0.0.1 grafana.microservices.local
127.0.0.1 prometheus.microservices.local
```

Ajoutez ces entrées à votre fichier hosts (`C:\Windows\System32\drivers\etc\hosts` sous Windows).

## 7. Vérifier l'exécution du GitHub Actions Runner

```bash
# Vérifier le statut des jobs récents dans le dossier du runner
cd C:\Dev\GitHubActions\actions-runner\_work
dir

# Vérifier les logs du runner si nécessaire
type C:\Dev\GitHubActions\actions-runner\_diag\*.log
```

Vous pouvez également vérifier le statut des workflows dans l'interface GitHub sous l'onglet "Actions" de votre dépôt.

## 8. Tester le service utilisateur

```bash
# Créer un utilisateur
curl -X POST -H "Content-Type: application/json" -d '{
  "name": "Test User",
  "email": "testuser@example.com"
}' http://api.microservices.local/users

# Lister les utilisateurs
curl http://api.microservices.local/users
```

Si l'Ingress ne fonctionne pas correctement, vous pouvez tester directement via un port-forward :

```bash
# Faire un port-forward du service utilisateur
kubectl port-forward svc/user-service 8081:8081

# Dans un nouveau terminal
curl -X POST -H "Content-Type: application/json" -d '{
  "name": "Test User",
  "email": "testuser@example.com"
}' http://localhost:8081/users
```

## 9. Tester le service de commande

Utilisez l'ID utilisateur obtenu précédemment :

```bash
# Créer une commande
curl -X POST -H "Content-Type: application/json" -d '{
  "userId": "ID_UTILISATEUR_OBTENU",
  "productId": "product-123",
  "amount": 49.99
}' http://api.microservices.local/orders

# Lister les commandes
curl http://api.microservices.local/orders
```

Ou avec port-forward si nécessaire :

```bash
kubectl port-forward svc/order-service 8082:8082

# Dans un nouveau terminal
curl -X POST -H "Content-Type: application/json" -d '{
  "userId": "ID_UTILISATEUR_OBTENU",
  "productId": "product-123",
  "amount": 49.99
}' http://localhost:8082/orders
```

## 10. Accéder au monitoring

### Via Ingress (méthode recommandée avec minikube tunnel)

Avec `minikube tunnel` en cours d'exécution, accédez directement aux interfaces via les noms d'hôte configurés :

- Grafana : [http://grafana.microservices.local](http://grafana.microservices.local)
    - Utilisateur par défaut : admin
    - Mot de passe par défaut : admin

- Prometheus : [http://prometheus.microservices.local](http://prometheus.microservices.local)

### Via Port-Forward (alternative)

Si vous rencontrez des problèmes avec l'Ingress :

```bash
# Accès à Grafana via port-forward
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80

# Accès à Prometheus via port-forward
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
```

Puis accédez via :
- Grafana : [http://localhost:3000](http://localhost:3000)
- Prometheus : [http://localhost:9090](http://localhost:9090)

## 11. Vérifier l'intégration Kafka

Après avoir créé une commande, vérifiez que l'ID de la commande a été ajouté à l'utilisateur :

```bash
curl http://api.microservices.local/users/ID_UTILISATEUR_OBTENU
```

La réponse devrait inclure l'ID de la commande dans le tableau `orderIds`.

## 12. Examiner les journaux des services

```bash
# Obtenir le nom du pod utilisateur et afficher ses logs
USER_POD=$(kubectl get pods -l app=user-service -o jsonpath="{.items[0].metadata.name}")
kubectl logs $USER_POD

# Pour le service de commande
ORDER_POD=$(kubectl get pods -l app=order-service -o jsonpath="{.items[0].metadata.name}")
kubectl logs $ORDER_POD
```

Recherchez les messages indiquant l'envoi et la réception d'événements Kafka.

## 13. Vérifier les métriques des applications

Dans Prometheus (http://prometheus.microservices.local), recherchez des métriques comme :
- `http_server_requests_seconds_count` (Spring Boot)
- `process_cpu_usage`
- `jvm_memory_used_bytes`

Dans Grafana (http://grafana.microservices.local), vous pouvez créer un nouveau tableau de bord simple avec ces requêtes :

1. Application latence :
   ```
   rate(http_server_requests_seconds_sum{job="user-service"}[1m]) / rate(http_server_requests_seconds_count{job="user-service"}[1m])
   ```

2. Requêtes par seconde :
   ```
   rate(http_server_requests_seconds_count{job="user-service"}[1m])
   ```

## 14. Astuces pour le dépannage spécifique à Minikube

### Problèmes avec minikube tunnel

Si l'Ingress ne fonctionne pas malgré le tunnel :

```bash
# Vérifiez l'état du tunnel
ps aux | grep "minikube tunnel"

# Redémarrez le tunnel si nécessaire
minikube tunnel --cleanup
minikube tunnel
```

### Problèmes d'Ingress

```bash
# Vérifier les pods du contrôleur nginx
kubectl get pods -n ingress-nginx

# Vérifier les événements d'ingress
kubectl describe ingress microservices-ingress
kubectl describe ingress monitoring-ingress -n monitoring
```

### Problèmes de stockage persistant

```bash
# Vérifier les PVCs
kubectl get pvc
kubectl describe pvc postgres-pvc
```

### Problèmes de ressources

```bash
# Vérifier l'utilisation des ressources par nœud
kubectl top node

# Vérifier l'utilisation des ressources par pod
kubectl top pod
```

### Redémarrer tous les composants

En cas de problèmes majeurs, vous pouvez redémarrer complètement les déploiements :

```bash
kubectl rollout restart deployment/user-service
kubectl rollout restart deployment/order-service
kubectl rollout restart deployment/postgres
kubectl rollout restart statefulset/kafka
```

### Relancer un déploiement via GitHub Actions

Pour re-déclencher le déploiement :

1. Allez dans l'onglet "Actions" de votre dépôt GitHub
2. Sélectionnez le workflow "Deploy to Kubernetes"
3. Cliquez sur "Run workflow" et choisissez la branche principale

Ou, modifiez un fichier dans le répertoire `k8s/` pour déclencher automatiquement le workflow.

## Conclusion

Votre déploiement est réussi si :

1. Tous les pods sont à l'état `Running`
2. Le tunnel Minikube fonctionne correctement et expose l'Ingress
3. Le GitHub Actions Runner est connecté et traite les jobs
4. Vous pouvez créer des utilisateurs et des commandes via l'API
5. Les messages Kafka sont correctement traités entre les services
6. Vous pouvez accéder aux métriques dans Prometheus/Grafana via l'ingress

N'oubliez pas que les fenêtres exécutant `minikube tunnel` et le GitHub Actions Runner (`.\run.cmd`) doivent rester ouvertes pendant toute la durée des tests pour assurer le bon fonctionnement de votre environnement.