# deploy.ps1 - Script pour reconstruire et redéployer les services sur Kubernetes

param (
    [string]$ServiceName = "order-service",
    [string]$Version = "0.1.1"
)

$DockerfilePath = "$ServiceName/Dockerfile.alt"

Write-Host "Déploiement de $ServiceName v$Version sur Kubernetes" -ForegroundColor Yellow

# Étape 1: Construction de l'image Docker
Write-Host "[1/5] Construction de l'image Docker..." -ForegroundColor Green
docker build -t $ServiceName`:$Version -f $DockerfilePath .

if (-not $?) {
    Write-Host "Erreur lors de la construction de l'image Docker. Arrêt du déploiement." -ForegroundColor Red
    exit 1
}

# Étape 2: Chargement de l'image dans Minikube
Write-Host "[2/5] Chargement de l'image dans Minikube..." -ForegroundColor Green
minikube image load $ServiceName`:$Version

# Étape 3: Mise à jour du déploiement Kubernetes
Write-Host "[3/5] Mise à jour du déploiement Kubernetes..." -ForegroundColor Green
kubectl set image deployment/$ServiceName $ServiceName=$ServiceName`:$Version

# Étape 3b: Forcer le redémarrage du déploiement pour utiliser la nouvelle image
Write-Host "[3b/5] Forcer le redémarrage du déploiement..." -ForegroundColor Green
kubectl rollout restart deployment/$ServiceName

# Étape 4: Vérification du déploiement
Write-Host "[4/5] Vérification du déploiement..." -ForegroundColor Green
kubectl rollout status deployment/$ServiceName

# Étape 5: Affichage des informations de port-forward
Write-Host "[5/5] Configuration du port-forward..." -ForegroundColor Green

# Déterminer le port à utiliser en fonction du service
if ($ServiceName -eq "order-service") {
    $Port = 8082
} elseif ($ServiceName -eq "user-service") {
    $Port = 8081
} else {
    $Port = 8080
}

Write-Host "Pour accéder au service, exécutez:" -ForegroundColor Yellow
Write-Host "kubectl port-forward svc/$ServiceName $Port`:$Port"

Write-Host "Pour voir les logs:" -ForegroundColor Yellow
Write-Host "kubectl logs -f deployment/$ServiceName"

Write-Host "Déploiement terminé!" -ForegroundColor Green