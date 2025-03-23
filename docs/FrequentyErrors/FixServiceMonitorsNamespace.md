# Fixing CI/CD and Monitoring Issues in a Kubernetes Microservices Setup

Great news! The images show that we've successfully fixed all the issues:

## Final Result
1. **Image 1**: Prometheus metrics collection is working correctly - both the order-service and user-service monitors are UP and scraping metrics from your services.
2. **Image 2**: GitHub Actions workflow completed successfully - the "Deploy to Kubernetes" workflow ran and completed in 7m 36s.

## Problem Overview and Solutions

### Initial Problem
Your GitHub Actions CI/CD pipeline could connect to Kubernetes but couldn't verify deployments were successful because pods were taking too long to become ready.

```
error: deployment "user-service" exceeded its progress deadline
error: deployment "order-service" exceeded its progress deadline
```

### Root Cause
Spring Boot applications have relatively long startup times, especially when connecting to databases and message brokers. The default timeouts in Kubernetes were too aggressive for this startup pattern.

### First Fix: Reset Minikube and Fix kubeconfig
We fixed the kubeconfig issues to establish proper connectivity between GitHub Actions and your Kubernetes cluster.

```powershell
# Reset Minikube to clean state
minikube delete
minikube start --driver=docker

# Update kubeconfig
$kubeconfig = Get-Content "$HOME/.kube/config" -Raw
$base64config = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($kubeconfig))
# Update this in GitHub Secrets
```

### Second Fix: Add Namespace Selectors to ServiceMonitors
We added namespace selectors to your ServiceMonitor resources so Prometheus could find your services in the default namespace.

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: user-service-monitor
  namespace: monitoring
  labels:
    release: monitoring
spec:
  namespaceSelector:  # Added this section
    matchNames:
      - default
  selector:
    matchLabels:
      app: user-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

We also added the proper labels to your services:

```bash
kubectl patch service user-service -p '{"metadata":{"labels":{"app":"user-service"}}}'
kubectl patch service order-service -p '{"metadata":{"labels":{"app":"order-service"}}}'
```

### Final Fix: Increase Timeouts in Kubernetes Resources
We increased the timeouts for various components to accommodate Spring Boot's startup characteristics:

1. **Deployment Readiness Probes**:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8081
  initialDelaySeconds: 300  # Increased from default
  periodSeconds: 30
  timeoutSeconds: 20
  failureThreshold: 10
```

2. **Added Startup Probes** to separate startup checks from regular readiness:
```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8081
  failureThreshold: 30
  periodSeconds: 10
```

3. **Increased Rollout Status Timeout** in the GitHub workflow:
```yaml
- name: Verify deployment
  shell: powershell
  run: |
    Write-Host "Verifying deployments..."
    kubectl rollout status deployment/user-service --timeout=15m
    kubectl rollout status deployment/order-service --timeout=15m
    Write-Host "All deployments successfully rolled out!"
```

## Key Insight
The mismatch between Spring Boot's startup characteristics and Kubernetes' default expectations for container readiness was the root cause of our issues. By adjusting timeout configurations, we created a more realistic deployment process that accommodates Spring Boot's initialization time.

You now have a fully functioning CI/CD pipeline with automated testing, containerization, deployment to Kubernetes, and monitoring with Prometheus and Grafana. Congratulations on successfully implementing this sophisticated DevOps setup!