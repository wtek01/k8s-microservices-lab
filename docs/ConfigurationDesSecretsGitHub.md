# Configuration des secrets GitHub

Pour que vos workflows GitHub Actions fonctionnent correctement, vous devez configurer les secrets suivants dans les paramètres de votre dépôt GitHub :

## Secrets Docker Hub

- `DOCKERHUB_USERNAME` : Votre nom d'utilisateur Docker Hub
- `DOCKERHUB_TOKEN` : Un token d'accès personnel Docker Hub (pas votre mot de passe)

Pour générer un token Docker Hub:
1. Connectez-vous à votre compte [Docker Hub](https://hub.docker.com)
2. Cliquez sur votre avatar > Account Settings > Security > New Access Token

## Secrets Kubernetes

- `KUBE_CONFIG` : Le contenu complet de votre fichier kubeconfig

Pour obtenir votre kubeconfig:
```bash
cat ~/.kube/config | base64
```

**Note**: Si vous utilisez un service cloud (EKS, AKS, GKE), vous aurez besoin de configurer différents secrets selon leur méthode d'authentification spécifique.

## Ajouter les secrets à GitHub

1. Allez dans votre dépôt GitHub
2. Cliquez sur "Settings" > "Secrets and variables" > "Actions"
3. Cliquez sur "New repository secret"
4. Ajoutez chaque secret avec son nom correspondant et sa valeur