# Intégration d'un Ingress Controller dans notre architecture microservices

Ce document explique les étapes suivies pour mettre en place un Ingress Controller afin d'exposer nos microservices (user-service et order-service) à l'extérieur du cluster Kubernetes.

## Objectif
L'Ingress Controller permet de router le trafic entrant vers différents services internes au cluster Kubernetes en fonction des règles définies, notamment basées sur le chemin d'URL ou le nom d'hôte. C'est une méthode plus élégante et configurable que l'utilisation de services de type NodePort ou LoadBalancer pour chaque microservice.

## Étapes d'implémentation

### 1. Installation de l'Ingress Controller NGINX

Nous avons d'abord installé l'Ingress Controller NGINX qui gérera les règles d'entrée pour notre cluster :

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

Vérification de l'installation :
```bash
kubectl get pods -n ingress-nginx
```

### 2. Activation de l'add-on Ingress dans Minikube

Comme nous utilisons Minikube, nous avons activé l'add-on Ingress :

```bash
minikube addons enable ingress
```

### 3. Création du fichier de configuration Ingress

Nous avons créé un fichier `k8s/ingress.yml` avec les règles de routage pour diriger le trafic vers nos microservices :

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: microservices-ingress
spec:
  ingressClassName: nginx
  rules:
    - host: api.microservices.local
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

Cette configuration définit que :
- Les requêtes vers `api.microservices.local/users` seront redirigées vers le service user-service sur le port 8081
- Les requêtes vers `api.microservices.local/orders` seront redirigées vers le service order-service sur le port 8082

### 4. Déploiement de la configuration Ingress

Nous avons appliqué la configuration Ingress au cluster :

```bash
kubectl apply -f k8s/ingress.yml
```

Vérification de la création de l'Ingress :
```bash
kubectl get ingress
```

### 5. Configuration du fichier hosts

Pour pouvoir accéder à notre API via le nom d'hôte défini, nous avons modifié le fichier hosts local :

```
127.0.0.1 api.microservices.local
```

Chemin du fichier hosts :
- Windows : `C:\Windows\System32\drivers\etc\hosts`
- Linux/Mac : `/etc/hosts`

### 6. Création d'un tunnel pour accéder à l'Ingress

Comme nous utilisons Minikube, nous avons créé un tunnel pour exposer l'Ingress sur localhost :

```bash
minikube tunnel
```

Cette commande doit rester active dans un terminal dédié pour maintenir le tunnel.

### 7. Test de l'Ingress

Nous avons testé l'accès à nos services via l'Ingress :

```bash
curl http://api.microservices.local/users
curl http://api.microservices.local/orders
```

Les requêtes ont été correctement routées vers nos microservices, confirmant le bon fonctionnement de notre configuration Ingress.

### 8. Intégration dans le workflow CI/CD

Pour automatiser le déploiement de l'Ingress lors des mises à jour de l'application, nous avons ajouté la ressource Ingress au workflow GitHub Actions en modifiant le fichier `.github/workflows/kubernetes-deploy-workflow.yml` :

```yaml
- name: Apply Kubernetes manifests
  shell: powershell
  run: |
    Write-Host "Applying Kubernetes configurations..."
    kubectl apply -f k8s/kafka-statefulset.yml
    kubectl apply -f k8s/postgres-deployment.yml
    kubectl apply -f k8s/user-service.yml
    kubectl apply -f k8s/order-service.yml
    kubectl apply -f k8s/ingress.yml  # Ajout de cette ligne
```

## Bénéfices de cette implémentation

- **Point d'entrée unique** pour accéder à tous nos microservices
- **Routage basé sur les chemins d'URL**, permettant une architecture API RESTful cohérente
- **Possibilité d'ajouter des fonctionnalités avancées** comme le SSL/TLS, l'authentification, etc.
- **Facilité d'extension** pour intégrer de nouveaux microservices à l'avenir

## Prochaines étapes (TODO)

Maintenant que notre Ingress est correctement configuré, nous pourrons :
- Ajouter la sécurité HTTPS en configurant des certificats
- Mettre en place des règles de rate limiting
- Configurer des redirections ou des règles de réécriture plus avancées si nécessaire