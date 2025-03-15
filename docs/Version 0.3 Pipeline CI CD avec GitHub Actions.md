Je vais vous aider à mettre en place un pipeline CI/CD avec GitHub Actions pour votre projet de microservices. Vous avez déjà bien avancé sur les versions 0.1 et 0.2 avec vos microservices Spring Boot, la communication Kafka, et le déploiement Kubernetes.

Passons à la mise en place du pipeline CI/CD (version 0.3) :

## Version 0.3 : Pipeline CI/CD avec GitHub Actions

Le pipeline que nous allons créer va :
1. Construire et tester vos microservices
2. Créer des images Docker
3. Pousser ces images vers un registre Docker
4. Générer/mettre à jour les manifestes Kubernetes
5. Déployer automatiquement sur votre cluster Kubernetes

### Structure du workflow GitHub Actions

Nous allons créer deux workflows :
1. Un workflow principal pour construire, tester et publier les images Docker
2. Un workflow de déploiement sur Kubernetes

Voici les fichiers à créer :

### Adaptation de vos Dockerfiles

Pour que vos Dockerfiles fonctionnent correctement avec GitHub Actions, il faut s'assurer qu'ils sont optimisés. Voici les Dockerfiles modifiés :

### Mise à jour des manifestes Kubernetes

Nous devons aussi ajuster vos manifestes Kubernetes pour qu'ils soient mis à jour par le pipeline :

## Explication du pipeline CI/CD (version 0.3)

Maintenant que j'ai créé tous les fichiers nécessaires, je vais vous expliquer en détail comment ce pipeline fonctionne :

### 1. Workflow principal (`main-ci-workflow.yml`)

Ce workflow est déclenché lorsque vous poussez du code sur les branches principales (main ou master). Il est composé de trois jobs :

#### a) Build et Test (`build-and-test`)
- Cette étape configure Java 17
- Exécute `mvn clean verify` pour construire et tester les microservices
- Archive les résultats des tests pour référence future
- Met en cache les dépendances Maven pour accélérer les builds futurs

#### b) Construction et publication des images Docker (`build-and-push-images`)
- Utilise une matrice pour traiter vos deux services en parallèle
- Se connecte à Docker Hub avec vos identifiants
- Extrait le numéro de version depuis votre pom.xml parent
- Construit les images Docker en utilisant les Dockerfiles optimisés
- Pousse les images vers Docker Hub avec le tag de version approprié
- Utilise le cache Docker pour accélérer les builds

#### c) Mise à jour des manifestes Kubernetes (`update-k8s-manifests`)
- Met à jour les manifestes Kubernetes pour pointer vers les nouvelles images
- Commit et pousse ces changements sur une branche dédiée `k8s-manifests`
- Cette action déclenche automatiquement le workflow de déploiement

### 2. Workflow de déploiement (`kubernetes-deploy-workflow.yml`)

Ce workflow est déclenché lorsque des changements sont poussés sur la branche `k8s-manifests` :

- Configure l'accès à votre cluster Kubernetes
- Applique les manifestes Kubernetes mis à jour
- Vérifie que les déploiements se sont correctement déroulés

### Les fichiers Dockerfile optimisés

J'ai aussi optimisé vos Dockerfiles pour qu'ils :
- Utilisent le cache de dépendances Maven efficacement
- Construisent les applications en multi-étapes pour des images plus légères
- Incluent un script wait-for-it pour gérer les dépendances au démarrage
- Utilisent un utilisateur non-root pour plus de sécurité

### Les manifestes Kubernetes

Les manifestes Kubernetes ont été ajustés pour :
- Utiliser l'image dynamique en provenance de Docker Hub
- Configurer `imagePullPolicy: Always` pour toujours prendre la dernière version
- Inclure les configurations nécessaires pour vos microservices

## Comment utiliser ce pipeline

1. **Créez la structure de dossiers GitHub Actions**
   ```bash
   mkdir -p .github/workflows
   ```

2. **Copiez les fichiers de workflow**
   Copiez `main-ci-workflow.yml` et `kubernetes-deploy-workflow.yml` dans `.github/workflows/`

3. **Configurez les secrets GitHub**
   Suivez les instructions dans le document "Configuration des secrets GitHub" pour ajouter les secrets nécessaires à votre dépôt GitHub

4. **Mettez à jour les Dockerfiles**
   Remplacez vos Dockerfiles existants par les versions optimisées

5. **Mettez à jour les manifestes Kubernetes**
   Remplacez vos manifestes K8s par les versions adaptées au pipeline CI/CD

6. **Lancez un premier pipeline**
   Poussez vos changements sur la branche principale pour déclencher le workflow
   ```bash
   git add .
   git commit -m "ci: configure GitHub Actions CI/CD pipeline"
   git push origin main
   ```

## Personnalisation possible

Vous pouvez personnaliser ce pipeline selon vos besoins spécifiques :

1. **Utiliser un registre Docker privé** au lieu de Docker Hub
2. **Ajouter des tests supplémentaires** (tests d'intégration, tests de performance)
3. **Configurer des stratégies de déploiement avancées** (blue/green, canary)
4. **Ajouter des notifications** (Slack, email) pour les succès/échecs de déploiement

## Avantages de cette approche

- **Automatisation complète** : du code au déploiement en production
- **Séparation des responsabilités** : build séparé du déploiement
- **Traçabilité** : versions d'images liées directement aux versions du projet
- **Sécurité** : utilisation de secrets pour les informations sensibles
- **Flexibilité** : facile à adapter pour différents environnements

Avec cette configuration, chaque fois que vous pousserez du code sur votre branche principale, vos microservices seront automatiquement testés, construits, empaquetés en images Docker et déployés sur votre cluster Kubernetes.

Avez-vous des questions spécifiques sur une partie du pipeline ou souhaitez-vous des ajustements pour mieux répondre à vos besoins ?