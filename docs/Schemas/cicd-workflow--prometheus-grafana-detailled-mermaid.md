### TODO sur https://excalidraw.com/
flowchart TD
subgraph DEV[Développement]
A[Développeur] -->|Git Push| B[GitHub Repository]
B -->|Trigger| C[GitHub Actions]
C -->|Execute| D[Self-hosted Runner]
end

    subgraph BUILD[Construction]
        D -->|Build & Test| E[Maven Build]
        E -->|Package| F[Docker Build]
        F -->|Push| G[DockerHub Registry]
    end
    
    subgraph DEPLOY[Déploiement]
        D -->|Deploy| H[kubectl apply]
        G -->|Pull Images| H
        H -->|Deploy manifests| I[Minikube Cluster]
    end
    
    subgraph KUBERNETES[Cluster Kubernetes]
        I --> I1[Ingress Controller]
        I --> I2[Services]
        I --> I3[Deployments]
        
        subgraph DATA[Stockage et Messaging]
            I4[PostgreSQL]
            I5[Kafka]
        end
        
        subgraph MONITORING[Monitoring Stack]
            M1[Prometheus]
            M2[Grafana]
            M1 -->|Query| M2
        end
        
        I1 --> I2
        I2 --> I3
        I3 --> DATA
        M1 -.->|Scrape Metrics| I2
        M1 -.->|Scrape Metrics| I3
    end
    
    subgraph ACCESS[Accès]
        U1[Client API] -->|http://api.microservices.local| I1
        U2[Admin] -->|Dashboards & Alerts| M2
    end
    
    classDef dev fill:#dff0d8,stroke:#5cb85c,stroke-width:2px;
    classDef build fill:#fcf8e3,stroke:#f0ad4e,stroke-width:2px;
    classDef deploy fill:#d9edf7,stroke:#5bc0de,stroke-width:2px;
    classDef k8s fill:#d9edf7,stroke:#5bc0de,stroke-width:2px;
    classDef monitor fill:#e8d5e4,stroke:#a64d79,stroke-width:2px;
    classDef access fill:#dff0d8,stroke:#5cb85c,stroke-width:2px;
    
    class A,B,C dev;
    class D,E,F build;
    class G,H,I deploy;
    class I1,I2,I3,I4,I5 k8s;
    class M1,M2 monitor;
    class U1,U2 access;