### Supprimer un deployement : 
- kubectl delete deployment order-service
### Appliquer les manifestes : 
- kubectl apply -f k8s/order-service.yml
### Vérifier si les pods démmarent correctement : 
- kubectl get pods -w
### Consulter les logs des pods : 
- kubectl logs user-service-788bbf48d7-pv7g5
### Pour voir les logs complets avec les erreurs
- kubectl logs user-service-788bbf48d7-pv7g5 --tail=100
### Pour voir les événements récents
- kubectl get events --sort-by=.metadata.creationTimestamp
### Vérifier si Kafka est en cours d'exécution
kubectl get pods | grep kafka
### Vérifier les informations détaillées sur le pod Kafka
kubectl describe pod kafka-0

### Vérifier que le service kafka existe bien
kubectl get service kafka

### Voir l'historique des déploiements dans votre cluster
kubectl rollout history ingress/microservices-ingress

###  vérifiez les événements récents liés à l'Ingress
kubectl describe ingress microservices-ingress
-  Regardez la section "Events" à la fin de la sortie

### MINIKUBE
- minikube start
- minikube tunnel
- minikube status
- minikube update-context (Cette commande va corriger votre fichier kubeconfig pour qu'il pointe vers le port correct.)