# Guide Complet des Commandes pour l'Environnement de Déploiement Automatisé

Ce document recense l'ensemble des commandes utilisées pour gérer notre environnement de déploiement automatisé avec Kubernetes (Minikube), incluant le monitoring avec Prometheus et Grafana, les microservices, Kafka, et toutes les ressources associées.

## Table des matières

1. [Commandes Minikube](#commandes-minikube)
2. [Commandes kubectl: Basiques](#commandes-kubectl-basiques)
3. [Commandes kubectl: Ressources](#commandes-kubectl-ressources)
4. [Commandes kubectl: Monitoring et Debugging](#commandes-kubectl-monitoring-et-debugging)
5. [Commandes Kafka](#commandes-kafka)
6. [Commandes Helm](#commandes-helm)
7. [Commandes Docker](#commandes-docker)
8. [Commandes GitHub Actions Runner](#commandes-github-actions-runner)
9. [Commandes pour la Manipulation de Fichiers/Données](#commandes-pour-la-manipulation-de-fichiersdonnées)

## Commandes Minikube

### Gestion du Cluster
```bash
# Démarrer Minikube
minikube start

# Arrêter Minikube
minikube stop

# Supprimer le cluster Minikube
minikube delete

# Vérifier le statut de Minikube
minikube status

# Obtenir l'adresse IP de Minikube
minikube ip

# Mettre à jour le contexte kubectl pour pointer vers Minikube
minikube update-context
```

### Gestion des Addons
```bash
# Lister tous les addons
minikube addons list

# Lister les addons avec filtrage
minikube addons list | grep ingress

# Activer un addon
minikube addons enable ingress

# Désactiver un addon
minikube addons disable ingress

# Vérifier que l'addon Ingress est bien activé
minikube addons list | grep ingress
```

### Configuration et Accès
```bash
# Démarrer un tunnel pour accéder aux services
minikube tunnel

# Accéder directement à un service
minikube service <service-name> --url
```

### Dashboard et Services
```bash
# Accéder au dashboard Kubernetes
minikube dashboard

# Lister tous les services exposés
minikube service list
```

## Commandes kubectl: Basiques

### Configuration et Contexte
```bash
# Afficher la configuration kubectl actuelle
kubectl config view

# Afficher la configuration minimale actuelle
kubectl config view --minify

# Afficher la configuration aplatie
kubectl config view --flatten

# Exporter la configuration dans un fichier
kubectl config view --minify --flatten > kubeconfig.yaml

# Définir un contexte comme courant
kubectl config use-context minikube

# Définir le namespace par défaut
kubectl config set-context --current --namespace=default
```

### Informations Générales
```bash
# Vérifier la version de kubectl
kubectl version --client

# Vérifier les informations sur le cluster
kubectl cluster-info

# Lister les nœuds du cluster
kubectl get nodes

# Lister les namespaces 
kubectl get namespaces
```

### Application des Manifests
```bash
# Appliquer un fichier de configuration
kubectl apply -f <fichier.yml>

# Appliquer plusieurs fichiers
kubectl apply -f <fichier1.yml> -f <fichier2.yml>

# Appliquer tous les fichiers d'un répertoire
kubectl apply -f <répertoire/>

# Appliquer un manifeste spécifique
kubectl apply -f k8s/order-service.yml

# Appliquer en utilisant kustomize
kubectl apply -k <répertoire/>

# Supprimer des ressources définies dans un fichier
kubectl delete -f <fichier.yml>
```

## Commandes kubectl: Ressources

### Pods
```bash
# Lister tous les pods
kubectl get pods

# Lister les pods avec des informations détaillées
kubectl get pods -o wide

# Lister les pods de tous les namespaces
kubectl get pods --all-namespaces

# Lister les pods avec un label spécifique
kubectl get pods -l app=user-service

# Obtenir les détails d'un pod
kubectl describe pod <nom-du-pod>

# Exécuter une commande dans un pod
kubectl exec -it <nom-du-pod> -- <commande>

# Obtenir les logs d'un pod
kubectl logs <nom-du-pod>

# Obtenir un nombre spécifique de lignes de logs
kubectl logs <nom-du-pod> --tail=100

# Suivre les logs en temps réel
kubectl logs -f <nom-du-pod>

# Surveiller les pods en temps réel
kubectl get pods -w
```

### Deployments
```bash
# Lister tous les déploiements
kubectl get deployments

# Obtenir les détails d'un déploiement
kubectl describe deployment <nom-du-déploiement>

# Mettre à l'échelle un déploiement
kubectl scale deployment <nom-du-déploiement> --replicas=3

# Redémarrer un déploiement
kubectl rollout restart deployment/<nom-du-déploiement>

# Vérifier le statut d'un déploiement
kubectl rollout status deployment/<nom-du-déploiement>

# Voir l'historique des déploiements
kubectl rollout history deployment/<nom-du-déploiement>

# Revenir à une version précédente
kubectl rollout undo deployment/<nom-du-déploiement> --to-revision=<numéro>

# Supprimer un déploiement
kubectl delete deployment <nom-du-déploiement>
```

### Services
```bash
# Lister tous les services
kubectl get services

# Lister les services de tous les namespaces
kubectl get services --all-namespaces

# Obtenir les détails d'un service
kubectl describe service <nom-du-service>

# Exposer un déploiement comme service
kubectl expose deployment <nom-du-déploiement> --port=8080 --target-port=8080 --type=ClusterIP

# Faire un port-forward vers un service
kubectl port-forward svc/<nom-du-service> <port-local>:<port-service>
```

### Ingress
```bash
# Lister tous les ingress
kubectl get ingress

# Lister les ingress de tous les namespaces
kubectl get ingress --all-namespaces

# Obtenir les détails d'un ingress
kubectl describe ingress <nom-de-l-ingress>

# Vérifier l'historique des déploiements de l'ingress
kubectl rollout history ingress/<nom-de-l-ingress>

# Voir les événements récents liés à l'ingress
kubectl describe ingress <nom-de-l-ingress>
```

### ConfigMaps et Secrets
```bash
# Lister tous les ConfigMaps
kubectl get configmaps

# Lister tous les Secrets
kubectl get secrets

# Créer un ConfigMap
kubectl create configmap <nom> --from-file=<chemin-du-fichier>

# Créer un Secret
kubectl create secret generic <nom> --from-literal=<clé>=<valeur>

# Décoder un Secret
kubectl get secret <nom> -o jsonpath="{.data.<clé>}" | base64 --decode
```

### StatefulSets et PersistentVolumeClaims
```bash
# Lister tous les StatefulSets
kubectl get statefulsets

# Lister tous les PersistentVolumeClaims
kubectl get pvc

# Lister tous les PersistentVolumes
kubectl get pv
```

### Ressources spécifiques au monitoring
```bash
# Lister les ServiceMonitors (ressources personnalisées de Prometheus)
kubectl get servicemonitor

# Lister les ServiceMonitors dans un namespace spécifique
kubectl get servicemonitor -n monitoring

# Obtenir les détails d'un ServiceMonitor
kubectl describe servicemonitor <nom> -n monitoring
```

## Commandes kubectl: Monitoring et Debugging

### Logs et Événements
```bash
# Voir les événements du cluster
kubectl get events

# Voir les événements triés par heure
kubectl get events --sort-by=.metadata.creationTimestamp

# Voir les logs d'un container spécifique dans un pod
kubectl logs <nom-du-pod> -c <nom-du-container>

# Voir les logs d'un déploiement
kubectl logs deployment/<nom-du-déploiement>

# Voir les logs filtrés par label
kubectl logs -l app=user-service
```

### Debugging
```bash
# Vérifier la santé d'un pod
kubectl exec <nom-du-pod> -- curl localhost:8080/actuator/health

# Créer un pod temporaire pour le debugging
kubectl run debug --image=busybox --rm -it -- sh

# Attendre qu'une condition soit remplie
kubectl wait --for=condition=ready pod -l app=user-service

# Vérifier la connectivité réseau
kubectl exec <nom-du-pod> -- wget -qO- <service>:<port>
```

### Métriques et Monitoring
```bash
# Obtenir les métriques des nœuds
kubectl top nodes

# Obtenir les métriques des pods
kubectl top pods

# Obtenir les métriques des pods dans tous les namespaces
kubectl top pods --all-namespaces
```

### Ressources et Quotas
```bash
# Lister les limites de ressources
kubectl get resourcequotas

# Lister les limites par namespace
kubectl get limitranges
```

## Commandes Kafka

### Administration de Kafka
```bash
# Vérifier si les pods Kafka sont en cours d'exécution
kubectl get pods | grep kafka

# Obtenir des informations détaillées sur le pod Kafka
kubectl describe pod kafka-0

# Vérifier que le service Kafka existe
kubectl get service kafka

# Exécuter une commande Kafka dans le pod
kubectl exec -it kafka-0 -- <commande-kafka>
```

### Gestion des Topics
```bash
# Créer un topic
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-topics --create --topic my-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1"

# Lister tous les topics
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-topics --list --bootstrap-server localhost:9092"

# Décrire un topic
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-topics --describe --topic my-topic --bootstrap-server localhost:9092"

# Supprimer un topic
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-topics --delete --topic my-topic --bootstrap-server localhost:9092"
```

### Gestion des Messages
```bash
# Produire des messages dans un topic
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-console-producer --topic my-topic --bootstrap-server localhost:9092"

# Consommer des messages depuis un topic (depuis le début)
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-console-consumer --topic my-topic --from-beginning --bootstrap-server localhost:9092"

# Consommer des messages en temps réel
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-console-consumer --topic my-topic --bootstrap-server localhost:9092"
```

### Gestion des Consumer Groups
```bash
# Lister les consumer groups
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-consumer-groups --list --bootstrap-server localhost:9092"

# Décrire un consumer group
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-consumer-groups --describe --group my-group --bootstrap-server localhost:9092"

# Réinitialiser les offsets d'un consumer group
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-consumer-groups --reset-offsets --to-earliest --execute --topic my-topic --group my-group --bootstrap-server localhost:9092"
```

### Monitoring et Performance
```bash
# Vérifier les partitions de Kafka
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-topics --describe --bootstrap-server localhost:9092"

# Visualiser les sous-réplications
kubectl exec -it kafka-0 -- /bin/sh -c "kafka-topics --describe --bootstrap-server localhost:9092 | grep -i 'under-replicated'"

# Vérifier les logs de Kafka
kubectl logs kafka-0

# Vérifier la santé de Kafka via JMX
kubectl exec -it kafka-0 -- /bin/sh -c "jconsole"
```

## Commandes Helm

### Installation et Configuration
```bash
# Vérifier la version de Helm
helm version

# Ajouter un repo Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Mettre à jour les repos
helm repo update

# Lister les repos
helm repo list
```

### Gestion des Charts
```bash
# Lister les charts installés
helm list

# Lister les charts dans un namespace spécifique
helm list -n monitoring

# Rechercher des charts
helm search repo prometheus

# Voir les valeurs par défaut d'un chart
helm show values prometheus-community/kube-prometheus-stack
```

### Installation et Mise à jour
```bash
# Installer un chart
helm install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring

# Installer avec des valeurs personnalisées
helm install monitoring prometheus-community/kube-prometheus-stack -f values.yaml --namespace monitoring

# Mettre à jour une installation
helm upgrade monitoring prometheus-community/kube-prometheus-stack --namespace monitoring

# Installer ou mettre à jour (si déjà installé)
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack --namespace monitoring
```

### Désinstallation
```bash
# Désinstaller un chart
helm uninstall monitoring -n monitoring

# Désinstaller et purger
helm uninstall --purge monitoring -n monitoring
```

## Commandes Docker

### Gestion des Images
```bash
# Construire une image
docker build -t <nom-image>:<tag> -f <chemin-dockerfile> <contexte>

# Construire une image pour le service user
docker build -t wtek01/user-service:0.3.0 -f user-service/Dockerfile .

# Lister les images
docker images

# Supprimer une image
docker rmi <nom-image>:<tag>
```

### Registre Docker
```bash
# Se connecter à DockerHub
docker login

# Pousser une image vers DockerHub
docker push <nom-image>:<tag>

# Tirer une image depuis DockerHub
docker pull <nom-image>:<tag>
```

### Exécution et Gestion des Conteneurs
```bash
# Exécuter un conteneur
docker run <nom-image>:<tag>

# Exécuter en mode interactif
docker run -it <nom-image>:<tag> /bin/bash

# Lister les conteneurs en cours d'exécution
docker ps

# Lister tous les conteneurs
docker ps -a

# Arrêter un conteneur
docker stop <container-id>

# Supprimer un conteneur
docker rm <container-id>
```

## Commandes GitHub Actions Runner

### Installation et Configuration
```bash
# Télécharger le runner
curl -o actions-runner-linux-x64-<version>.tar.gz -L https://github.com/actions/runner/releases/download/v<version>/actions-runner-linux-x64-<version>.tar.gz

# Extraire l'archive
tar xzf ./actions-runner-linux-x64-<version>.tar.gz

# Configurer le runner
./config.cmd --url https://github.com/<OWNER>/<REPO> --token <TOKEN>
```

### Gestion du Runner
```bash
# Démarrer le runner manuellement
./run.cmd

# Installer comme service (Windows)
./svc.cmd install

# Démarrer le service (Windows)
./svc.cmd start

# Arrêter le service (Windows)
./svc.cmd stop

# Désinstaller le service (Windows)
./svc.cmd uninstall
```

## Commandes pour la Manipulation de Fichiers/Données

### Manipulation de Base64
```bash
# Encoder en base64
echo -n "texte" | base64

# Décoder du base64
echo "dGV4dGU=" | base64 --decode

# PowerShell: Encoder en base64
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("texte"))

# PowerShell: Décoder du base64
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("dGV4dGU="))
```

### Manipulation de Fichiers et Filtrage
```bash
# Rechercher dans les fichiers
grep "recherche" <fichier>

# Filtrer les résultats
kubectl get pods | grep user-service

# Sélectionner une colonne spécifique
kubectl get pods -o jsonpath='{.items[*].metadata.name}'

# Obtenir une valeur spécifique d'un JSON/YAML
kubectl get secret <nom> -o jsonpath="{.data.password}"

# Modifier un fichier YAML avec sed
sed -i 's/ancienne-valeur/nouvelle-valeur/' <fichier.yml>

# PowerShell: Modifier un fichier
(Get-Content <fichier.yml>) -replace "ancienne-valeur", "nouvelle-valeur" | Set-Content <fichier.yml>
```

### Requêtes HTTP pour Testing
```bash
# Faire une requête GET
curl http://localhost:8081/users

# Faire une requête GET avec un en-tête Host
curl -H "Host: api.microservices.local" http://localhost/users

# Faire une requête POST avec des données JSON
curl -X POST -H "Content-Type: application/json" -d '{"name":"test","email":"test@example.com"}' http://localhost:8081/users
```

---

Ce guide de référence recense les principales commandes utilisées pour gérer notre environnement de déploiement automatisé. Vous pouvez l'utiliser comme référence rapide pour les opérations courantes sur votre infrastructure Kubernetes.