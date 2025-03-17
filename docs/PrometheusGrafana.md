# Prometheus et Grafana : Monitoring pour Kubernetes

## Introduction

Le monitoring est un aspect essentiel de toute infrastructure de microservices moderne. Pour assurer la fiabilité, la performance et la disponibilité de vos applications, vous avez besoin d'une visibilité complète sur votre système. C'est là que Prometheus et Grafana entrent en jeu, formant ensemble une solution de monitoring puissante et flexible pour les environnements Kubernetes.

## Prometheus : La Collection de Métriques

### Qu'est-ce que Prometheus ?

Prometheus est un système open-source de monitoring et d'alerte initialement développé par SoundCloud. Il est maintenant un projet gradué de la Cloud Native Computing Foundation (CNCF), aux côtés de Kubernetes.

### Caractéristiques principales

- **Modèle de données multidimensionnel** : Les métriques sont identifiées par un nom et des paires clé-valeur (labels)
- **PromQL** : Un langage de requête flexible permettant d'exploiter ces dimensions
- **Architecture autonome** : Ne dépend pas d'un stockage distribué
- **Collection par "scraping"** : Récupère les métriques via HTTP à intervalles réguliers
- **Service discovery** : Découverte automatique des cibles à surveiller
- **Support pour les séries temporelles** : Stockage efficace des données de métriques

### Fonctionnement dans Kubernetes

Dans un environnement Kubernetes, Prometheus fonctionne ainsi :

1. **Déploiement** : Prometheus est déployé en tant que StatefulSet dans votre cluster
2. **Configuration** : Il découvre automatiquement les services à surveiller via le service discovery de Kubernetes
3. **Collecte** : Il scrape les endpoints `/metrics` exposés par vos applications et composants Kubernetes
4. **Stockage** : Les métriques sont stockées localement dans une base de données time-series
5. **Requêtes** : Les données peuvent être interrogées via l'API ou l'interface utilisateur de Prometheus

### Types de métriques

Prometheus collecte quatre types principaux de métriques :

- **Counter** : Valeur cumulative qui ne peut qu'augmenter (ex: nombre total de requêtes)
- **Gauge** : Valeur qui peut augmenter et diminuer (ex: utilisation mémoire)
- **Histogram** : Échantillons d'observations regroupés dans des buckets (ex: latence de requêtes)
- **Summary** : Similaire à l'histogram mais fournit des quantiles calculés côté client

## Grafana : La Visualisation de Données

### Qu'est-ce que Grafana ?

Grafana est une plateforme d'analyse et de visualisation de métriques open-source. Elle permet de créer des tableaux de bord interactifs et personnalisables à partir de diverses sources de données, dont Prometheus.

### Caractéristiques principales

- **Tableaux de bord dynamiques** : Visualisations riches et interactives
- **Support multi-sources** : Compatible avec de nombreuses sources de données (Prometheus, InfluxDB, Elasticsearch, etc.)
- **Alertes** : Définition de règles d'alerte basées sur les métriques
- **Annotations** : Marquage d'événements importants sur les graphiques
- **Partage** : Export et partage faciles des tableaux de bord
- **Authentification** : Intégration avec divers systèmes d'authentification (LDAP, OAuth, etc.)

### Intégration avec Prometheus

L'intégration entre Grafana et Prometheus est simple et puissante :

1. **Configuration de la source de données** : Ajout de Prometheus comme source de données dans Grafana
2. **Création de tableaux de bord** : Utilisation de PromQL pour interroger les métriques
3. **Visualisation** : Affichage des données sous forme de graphiques, jauges, tables, etc.
4. **Alertes** : Configuration d'alertes basées sur les seuils définis

## Architecture de Monitoring dans Kubernetes

Une architecture typique de monitoring avec Prometheus et Grafana dans Kubernetes inclut :

```
┌─────────────────────────────────────┐
│            Applications             │
│  ┌───────────┐       ┌───────────┐  │
│  │User Service│       │Order Service│  │
│  │ /metrics   │       │ /metrics   │  │
│  └───────────┘       └───────────┘  │
└─────────────────────────────────────┘
           ▲                  ▲
           │                  │
           │     Scraping     │
           │                  │
┌──────────▼──────────────────▼───────┐
│              Prometheus             │
│                                     │
│  ┌───────────┐      ┌───────────┐   │
│  │Time-series│      │  AlertMgr │   │
│  │   DB      │      │           │   │
│  └───────────┘      └───────────┘   │
└─────────────────────────────────────┘
           ▲
           │      Requêtes
           │
┌──────────▼──────────────────────────┐
│               Grafana               │
│                                     │
│  ┌───────────┐      ┌───────────┐   │
│  │ Dashboards│      │   Alerts  │   │
│  │           │      │           │   │
│  └───────────┘      └───────────┘   │
└─────────────────────────────────────┘
           ▲
           │      Visualisation
           │
┌──────────▼──────────────────────────┐
│          Utilisateurs               │
└─────────────────────────────────────┘
```

## Métriques essentielles pour les microservices

Pour les microservices Spring Boot, voici quelques métriques importantes à surveiller :

### Métriques d'infrastructure
- **CPU** : Utilisation et saturation
- **Mémoire** : Utilisation, garbage collection
- **Disque** : Espace disponible, opérations I/O
- **Réseau** : Trafic entrant/sortant, erreurs

### Métriques d'application
- **Requêtes HTTP** : Nombre, taux d'erreurs, latence
- **Connections JVM** : Heap memory, threads, garbage collection
- **Requêtes de base de données** : Nombre, durée, erreurs
- **Messages Kafka** : Débit de production/consommation, lag de consommation

### Métriques métier
- **Taux de création de commandes**
- **Délai de traitement des commandes**
- **Nombre d'utilisateurs actifs**

## Configuration de Spring Boot pour Prometheus

Pour exposer les métriques de vos applications Spring Boot, vous devez ajouter les dépendances suivantes :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Et configurer application.yml :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

## Dashboards Grafana recommandés

Pour les microservices Spring Boot, ces tableaux de bord sont particulièrement utiles :

1. **JVM Overview** : Surveillance de la JVM (mémoire, threads, garbage collection)
2. **Spring Boot Statistics** : Métriques spécifiques à Spring Boot
3. **HTTP Request Overview** : Taux de requêtes, erreurs, latence
4. **Kubernetes Pod Resources** : Utilisation des ressources par pod
5. **Kafka Overview** : Métriques de production et consommation de messages

## Conclusion

L'utilisation de Prometheus et Grafana dans un environnement Kubernetes offre une solution complète pour le monitoring de vos microservices. Cette stack vous permet de :

- **Détecter proactivement** les problèmes avant qu'ils n'affectent vos utilisateurs
- **Diagnostiquer rapidement** les incidents quand ils surviennent
- **Optimiser** les performances de vos applications
- **Comprendre** les comportements et tendances de votre système

En implémentant cette solution dans votre architecture, vous franchissez une étape importante vers une infrastructure plus robuste, observable et fiable.