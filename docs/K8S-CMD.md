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