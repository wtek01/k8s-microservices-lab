I'm glad everything is running correctly now! Here's a brief explanation of what the problem was and how we resolved it:

### The Problem

Your GitHub Actions CI/CD workflow was failing at the deployment stage because it couldn't connect to your Kubernetes cluster. The specific error was:

```
Unable to connect to the server: dial tcp [::1]:8080: connectex: No connection could be made because the target machine actively refused it.
```

This happened because:
1. Your kubeconfig file was pointing to the wrong Kubernetes API server address
2. There were encoding issues with the kubeconfig file
3. Your Minikube installation was in an unhealthy state

### The Solution

We solved the problem through these steps:

1. **Fixed the kubeconfig file**: We completely recreated your Minikube installation to ensure a clean, properly configured Kubernetes environment.

2. **Reset Minikube**:
   ```
   minikube delete
   minikube start --driver=docker
   ```
   This created a fresh Kubernetes cluster without the configuration issues.

3. **Updated the GitHub Secret**: After Minikube was properly running, we generated a new base64-encoded kubeconfig and updated the KUBE_CONFIG secret in your GitHub repository.

4. **Restored the original workflow file**: We went back to the standard workflow configuration that uses the kubeconfig directly without the proxy approach.

5. **Installed Prometheus & CRDs**: We installed the Prometheus Operator using Helm to add the necessary CustomResourceDefinitions for your ServiceMonitor resources.

6. **Setup proper access**: We ensured Minikube tunnel was running and properly configured your hosts file to access the services.

The key insight was that your Kubernetes cluster needed to be properly initialized and configured before the CI/CD pipeline could deploy to it. Once we fixed the underlying Kubernetes environment, the GitHub Actions workflow was able to successfully connect and deploy your microservices.