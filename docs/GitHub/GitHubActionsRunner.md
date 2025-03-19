# GitHub Actions Runner: Documentation

## Introduction

GitHub Actions Runner est un service qui permet d'exécuter des workflows GitHub Actions sur votre propre infrastructure. Cette approche "self-hosted" offre plus de flexibilité et de contrôle sur l'environnement d'exécution par rapport aux runners hébergés par GitHub.

## Objectifs et cas d'utilisation

Les runners auto-hébergés sont particulièrement utiles dans les scénarios suivants :

- Nécessité d'accéder à des ressources internes (réseaux, serveurs, clusters Kubernetes)
- Besoins matériels spécifiques non disponibles sur les runners GitHub
- Configuration d'environnement personnalisée pour vos workflows
- Économie sur les minutes de calcul (pour les dépôts privés)
- Exécution de tâches sur des architectures spécifiques

## Installation et configuration

### Prérequis

- Un compte GitHub avec accès à un dépôt
- Un serveur ou une machine avec :
   - Windows, Linux ou macOS
   - Connectivité réseau pour accéder à GitHub
   - Accès à toutes les ressources requises par vos workflows

### Étapes d'installation

1. **Création du runner dans GitHub**

   - Accédez à votre dépôt GitHub → Settings → Actions → Runners
   - Cliquez sur "New self-hosted runner"
   - Sélectionnez votre système d'exploitation et architecture

2. **Installation sur votre machine**

   Pour Windows :
   ```powershell
   # Créer un dossier pour le runner
   mkdir actions-runner && cd actions-runner
   
   # Télécharger l'application
   Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.312.0/actions-runner-win-x64-2.312.0.zip -OutFile actions-runner-win-x64-2.312.0.zip
   
   # Extraire le contenu
   Add-Type -AssemblyName System.IO.Compression.FileSystem ; [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/actions-runner-win-x64-2.312.0.zip", "$PWD")
   
   # Configurer le runner
   ./config.cmd --url https://github.com/[OWNER]/[REPO] --token [TOKEN]
   ```

3. **Démarrage du runner**

   ```bash
   # Démarrer le runner manuellement
   ./run.cmd
   ```

4. **Configuration en tant que service** (recommandé pour la production)

   Pour Windows :
   ```powershell
   # Installer en tant que service
   ./svc.cmd install
   
   # Démarrer le service
   ./svc.cmd start
   ```

   Pour Linux :
   ```bash
   # Installer en tant que service avec systemd
   sudo ./svc.sh install
   
   # Démarrer le service
   sudo ./svc.sh start
   ```

## Intégration avec Kubernetes

Pour un runner qui déploie sur Kubernetes (comme dans votre cas) :

1. **Configuration de l'accès Kubernetes**

   Assurez-vous que la machine où s'exécute le runner a :
   - `kubectl` installé
   - Un fichier `kubeconfig` correctement configuré
   - Les autorisations nécessaires pour déployer des ressources

2. **Configurations sécurisées**

   Dans votre dépôt GitHub, configurez les secrets nécessaires :
   ```
   KUBE_CONFIG: [Base64 encoded kubeconfig]
   ```

3. **Utilisation dans les workflows**

   ```yaml
   jobs:
     deploy:
       runs-on: self-hosted
       steps:
         - name: Setup kubeconfig
           run: |
             echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > $HOME/.kube/config
             chmod 600 $HOME/.kube/config
         
         - name: Deploy to Kubernetes
           run: kubectl apply -f k8s/
   ```

## Au-delà de Minikube local

Les runners auto-hébergés peuvent être déployés dans divers environnements et utilisés pour de nombreuses finalités :

### Environnements Cloud
- **Clusters Kubernetes Cloud** : Déploiement sur AKS, EKS, GKE ou tout service Kubernetes managé
- **VMs Cloud** : Exécution sur EC2, Azure VMs, ou Google Compute Engine
- **CI/CD Cloud-Native** : Intégration avec les services natifs des fournisseurs cloud

### Infrastructure On-Premises
- **Centres de données** : Déploiement sur des clusters Kubernetes on-premises ou des fermes de serveurs traditionnelles
- **Edge Computing** : Déploiement sur des emplacements edge avec connectivité GitHub limitée
- **Environnements hybrides** : Pont entre déploiements cloud et on-premises

### Cas d'utilisation spécialisés
- **Tests matériels spécifiques** : Exécution de tests sur des configurations matérielles spécifiques
- **Déploiements IoT** : Déploiement d'applications sur des passerelles IoT ou appareils spécialisés
- **Environnements haute sécurité** : Exécution dans des réseaux air-gapped ou hautement restreints
- **Accès GPU/Matériel spécialisé** : Accès à des ressources de calcul spécialisées pour ML/IA

### Applications d'entreprise
- **Déploiements multi-clusters** : Coordination des déploiements sur plusieurs clusters Kubernetes
- **Déploiements Blue/Green** : Gestion de stratégies de déploiement sophistiquées
- **Releases Canary** : Implémentation de stratégies de release complexes
- **Environnements conformes** : Exécution de workflows dans des environnements avec exigences de conformité spécifiques (HIPAA, PCI, etc.)

### Avantages additionnels
- **Dépendances logicielles personnalisées** : Exécution dans des environnements avec dépendances ou licences spécifiques
- **Isolation réseau** : Accès aux réseaux privés sans les exposer à internet
- **Opérations intensives en ressources** : Réalisation d'opérations nécessitant plus de ressources que les runners GitHub
- **Opérations longue durée** : Exécution de jobs dépassant les limites de temps des runners GitHub

L'approche runner auto-hébergé offre une flexibilité considérable, permettant à vos pipelines CI/CD d'interagir avec pratiquement n'importe quelle infrastructure ou cible de déploiement, pas uniquement des clusters Minikube locaux.

## Maintenance et gestion

### Mise à jour du runner

Pour maintenir le runner à jour :

```bash
# Arrêter le service si configuré comme service
./svc.cmd stop  # Windows
sudo ./svc.sh stop  # Linux

# Mise à jour
cd actions-runner
./config.cmd remove --token [TOKEN]
# Télécharger et installer la nouvelle version
./config.cmd --url https://github.com/[OWNER]/[REPO] --token [TOKEN]

# Redémarrer le service
./svc.cmd start  # Windows
sudo ./svc.sh start  # Linux
```

### Surveillance

- Vérifiez régulièrement l'état du runner dans l'interface GitHub
- Configurez des alertes pour être notifié en cas de problème avec le runner
- Consultez les logs pour diagnostiquer les problèmes :
   - Windows : `_diag` folder ou journaux d'événements Windows
   - Linux : `_diag` folder ou journaux systemd si installé en tant que service

## Bonnes pratiques

1. **Sécurité**
   - Exécutez le runner avec le principe du moindre privilège
   - Utilisez des secrets GitHub pour les informations sensibles
   - Isolez le runner dans un environnement contrôlé

2. **Haute disponibilité**
   - Configurez plusieurs runners pour éviter les points de défaillance uniques
   - Mettez en place une surveillance pour détecter les runners défaillants

3. **Gestion des ressources**
   - Assurez-vous que le runner dispose de ressources suffisantes
   - Envisagez d'utiliser des étiquettes pour diriger différents types de workloads

4. **Mises à jour régulières**
   - Gardez votre runner à jour pour bénéficier des dernières fonctionnalités et correctifs de sécurité

## Résolution des problèmes courants

| Problème | Solution possible |
|----------|-------------------|
| Le runner ne démarre pas | Vérifiez les logs dans le dossier `_diag`, assurez-vous que le token est valide |
| Le runner ne peut pas se connecter à GitHub | Vérifiez la connectivité réseau et les paramètres de proxy |
| Les workflows échouent | Examinez les logs des jobs pour identifier les erreurs spécifiques |
| Problèmes d'accès aux ressources | Vérifiez les permissions du compte sous lequel s'exécute le runner |

## Cas d'utilisation spécifique : Microservices Spring Boot avec Minikube

Dans le cadre de ce projet, le GitHub Actions Runner est utilisé dans une configuration particulière :

### Configuration actuelle
- **Environnement local** : Le runner s'exécute sur une machine locale Windows, connecté au même environnement où Minikube est déployé
- **Pipeline CI/CD** : Déclenché automatiquement lors des push sur les branches main/master ou manuellement via workflow_dispatch
- **Intégration Minikube** : Le runner déploie directement sur un cluster Kubernetes local géré par Minikube
- **Automatisation complète** : Le cycle entier depuis le commit du code jusqu'au déploiement en Kubernetes est automatisé

### Rôle central du runner dans le pipeline

Le GitHub Actions Runner est l'élément central qui exécute toutes les étapes du workflow CI/CD :

1. **Déclenchement** : Lorsqu'un commit est poussé vers GitHub ou qu'un workflow est déclenché manuellement, GitHub envoie une notification au runner local.

2. **Récupération des instructions** : Le runner local (exécuté via run.cmd) télécharge les instructions de workflow depuis les fichiers `.github/workflows/*.yml` dans le dépôt.

3. **Exécution séquentielle** : Le runner exécute ensuite chaque étape du workflow dans l'ordre défini :
   - Récupération du code source
   - Compilation et tests avec Maven
   - Construction des images Docker
   - Publication des images vers DockerHub
   - Configuration de kubectl avec les credentials nécessaires
   - Déploiement des manifestes Kubernetes sur Minikube
   - Redémarrage forcé des services pour utiliser les nouvelles images
   - Vérification que le déploiement s'est correctement terminé

4. **Interaction directe avec Kubernetes** : L'avantage majeur est que le runner s'exécute sur la même machine que Minikube, permettant une interaction directe avec le cluster sans problèmes de connectivité ou d'authentification complexes.

### Flux de travail
1. Le développeur effectue un commit et push vers GitHub
2. GitHub Actions déclenche le workflow configuré dans `.github/workflows/`
3. Le runner auto-hébergé local exécute le pipeline :
   - Checkout du code
   - Configuration de kubectl
   - Application des fichiers Kubernetes (`.yml`) dans le répertoire `k8s/`
   - Redémarrage forcé des déploiements pour utiliser les nouvelles images
   - Vérification du déploiement

### Avantages dans ce contexte
- **Développement local rapide** : Permet de tester l'infrastructure CI/CD complète sans dépendre de services cloud
- **Apprentissage et formation** : Idéal pour apprendre les concepts DevOps dans un environnement contrôlé
- **Validation de configuration** : Les fichiers de configuration Kubernetes sont validés dans un environnement réel
- **Préparation au cloud** : La même configuration pourra être adaptée pour un déploiement cloud ultérieur

### Évolution possible (TODO)
Cette configuration constitue une excellente fondation qui peut évoluer vers :
- Un déploiement sur cloud public (AWS, Azure, GCP)
- Une configuration multi-environnements (dev, staging, production)
- L'ajout de tests automatisés et de validation de qualité
- L'implémentation de stratégies de déploiement avancées (bleu-vert, canary)

## Conclusion

GitHub Actions Runner auto-hébergé est un outil puissant qui vous donne un contrôle total sur vos environnements de CI/CD. En l'intégrant avec Kubernetes, vous pouvez créer un pipeline de déploiement automatisé robuste qui répond parfaitement à vos besoins spécifiques.

Dans votre architecture, le runner joue un rôle crucial en permettant l'application automatique des configurations Kubernetes, assurant ainsi que vos microservices sont déployés de manière cohérente et fiable à chaque mise à jour de code.