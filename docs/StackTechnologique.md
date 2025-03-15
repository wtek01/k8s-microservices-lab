# Stack Technologique Complet du Projet de Microservices avec CI/CD

Ce document présente l'ensemble des technologies et outils utilisés dans notre projet de microservices Spring Boot avec déploiement automatisé sur Kubernetes via GitHub Actions.

## Développement
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **Java** | 17 | Langage de programmation principal |
| **Spring Boot** | 3.x | Framework pour le développement des microservices |
| **Spring Data JPA** | - | Persistance des données et accès aux bases de données |
| **Spring Kafka** | - | Communication asynchrone entre services |
| **Lombok** | - | Réduction du code boilerplate (@Slf4j, @Data, etc.) |
| **Maven** | - | Gestion des dépendances et build |

## Base de données
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **PostgreSQL** | 14.x | Système de gestion de base de données relationnelle |
| **Flyway** | - | Migrations de schéma de base de données |

## Messagerie
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **Apache Kafka** | - | Broker de messagerie pour la communication asynchrone |
| **Zookeeper** | - | Coordination des nœuds Kafka |

## Conteneurisation
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **Docker** | - | Conteneurisation des applications |
| **Dockerfile** | - | Définition des images de conteneurs |
| **Docker Hub** | - | Registre pour stocker les images Docker |
| **Docker Buildx** | - | Extension pour construire des images multi-plateforme |

## Orchestration
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **Kubernetes** | - | Orchestration des conteneurs |
| **Minikube** | - | Distribution Kubernetes locale pour le développement |
| **kubectl** | - | Outil CLI pour interagir avec Kubernetes |
| **Kustomize** | - | Gestion des configurations Kubernetes |
| **kubeconfig** | - | Fichier de configuration pour se connecter au cluster Kubernetes |

## CI/CD
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **GitHub** | - | Hébergement du code source et gestion de versions |
| **GitHub Actions** | - | Pipeline d'intégration et déploiement continus |
| **GitHub Actions Runner auto-hébergé** | - | Exécution des workflows sur machine locale |
| **Git** | - | Système de contrôle de version |
| **Actions Checkout** | v3 | Récupération du code source |
| **Actions Setup-Java** | v3 | Configuration de l'environnement Java |
| **Actions Docker-Login** | v2 | Authentification auprès de Docker Hub |
| **Actions Docker-Buildx** | v2 | Construction des images Docker |
| **Actions Setup-Kubectl** | v3 | Installation de kubectl |

## Configuration et monitoring
| Technologie | Version | Utilisation |
|-------------|---------|-------------|
| **ConfigMaps** | - | Gestion de la configuration Kubernetes |
| **Secrets** | - | Gestion sécurisée des informations sensibles |
| **Actuator** | - | Endpoints de santé et de monitoring pour Spring Boot |
| **Health Checks** | - | Vérifications de santé pour les conteneurs |

## Manifestes Kubernetes
| Ressource | Utilisation |
|-----------|-------------|
| **Deployments** | Pour les services applicatifs |
| **StatefulSets** | Pour Kafka qui nécessite un état persistant |
| **Services** | Pour exposer les applications |
| **PersistentVolumeClaims** | Pour le stockage persistant |
| **ConfigMaps et Secrets** | Pour la configuration |

## Workflows GitHub Actions
| Workflow | Utilisation |
|----------|-------------|
| **main-ci-workflow.yml** | Construction, test et publication des images |
| **kubernetes-deploy-workflow.yml** | Déploiement sur Kubernetes |

## Langages de script
| Langage | Utilisation |
|---------|-------------|
| **PowerShell** | Scripts dans le runner auto-hébergé Windows |
| **YAML** | Configuration des workflows et manifestes Kubernetes |
| **Bash** | Scripts dans les workflows GitHub Actions |

## Outils de test
| Outil | Utilisation |
|-------|-------------|
| **JUnit** | Framework de test pour Java |
| **Spring Test** | Outils de test pour les applications Spring |
| **Testcontainers** | Tests d'intégration avec des conteneurs |

## Autres
| Technologie | Utilisation |
|-------------|-------------|
| **Base64** | Encodage et décodage du fichier kubeconfig |
| **Port-forward** | Accès aux services dans le cluster Kubernetes |
| **Rolling Updates** | Stratégie de déploiement pour les mises à jour sans interruption |

---

Ce stack technologique complet permet la mise en place d'un pipeline CI/CD automatisé pour le développement, la construction, le test et le déploiement de microservices Spring Boot sur Kubernetes.