# Modular Deployment Guide

This guide explains how to deploy individual services independently using separate Helm charts and GitHub Actions workflows.

## Overview

Each service now has its own:
- **Helm Chart**: Located in `infra/helm-charts/{service-name}/`
- **GitHub Actions Workflow**: Located in `.github/workflows/deploy-{service-name}.yml`
- **Independent Deployment**: Can be built and deployed separately

## Available Services

### 1. Auth Service
- **Chart**: `infra/helm-charts/auth-service/`
- **Workflow**: `.github/workflows/deploy-auth-service.yml`
- **Trigger**: Changes to `server/auth-service/` or `infra/helm-charts/auth-service/`
- **Image**: `ghcr.io/aet-devops25/team-3/auth-service:latest`

### 2. Document Service
- **Chart**: `infra/helm-charts/document-service/`
- **Workflow**: `.github/workflows/deploy-document-service.yml`
- **Trigger**: Changes to `server/document-service/` or `infra/helm-charts/document-service/`
- **Image**: `ghcr.io/aet-devops25/team-3/document-service:latest`

### 3. GenAI Service (Kotlin)
- **Chart**: `infra/helm-charts/genai-service/`
- **Workflow**: `.github/workflows/deploy-genai-service.yml`
- **Trigger**: Changes to `server/genai-service/` or `infra/helm-charts/genai-service/`
- **Image**: `ghcr.io/aet-devops25/team-3/genai-service:latest`

### 4. GenAI Backend (Python)
- **Chart**: `infra/helm-charts/genai-backend/`
- **Workflow**: `.github/workflows/deploy-genai-backend.yml`
- **Trigger**: Changes to `genAi/` or `infra/helm-charts/genai-backend/`
- **Image**: `ghcr.io/aet-devops25/team-3/genai:latest`

### 5. Client (React)
- **Chart**: `infra/helm-charts/client/`
- **Workflow**: `.github/workflows/deploy-client.yml`
- **Trigger**: Changes to `client/` or `infra/helm-charts/client/`
- **Image**: `ghcr.io/aet-devops25/team-3/client:latest`

## How to Deploy Individual Services

### Option 1: Automatic Deployment (Recommended)

1. **Make changes to a service**:
   ```bash
   # Edit auth service code
   vim server/auth-service/src/main/kotlin/de/tum/cit/aet/auth/controller/AuthController.kt
   ```

2. **Commit and push**:
   ```bash
   git add .
   git commit -m "Update auth service endpoint"
   git push origin main
   ```

3. **GitHub Actions automatically**:
   - Builds the service
   - Creates Docker image
   - Pushes to registry
   - Deploys to Kubernetes

### Option 2: Manual Deployment

#### Deploy Auth Service Only
```bash
# Build and push image
cd server/auth-service
docker build -t ghcr.io/aet-devops25/team-3/auth-service:latest .
docker push ghcr.io/aet-devops25/team-3/auth-service:latest

# Deploy to Kubernetes
helm upgrade --install auth-service ./infra/helm-charts/auth-service -n study-mate \
  --set image.tag=latest \
  --set postgres.host="study-mate-postgres" \
  --set postgres.port=5432 \
  --set postgres.database="auth_db" \
  --set postgres.username="postgres" \
  --set postgres.secretName="study-mate-postgres" \
  --set jwt.secretName="auth-secret"
```

#### Deploy Document Service Only
```bash
# Build and push image
cd server/document-service
docker build -t ghcr.io/aet-devops25/team-3/document-service:latest .
docker push ghcr.io/aet-devops25/team-3/document-service:latest

# Deploy to Kubernetes
helm upgrade --install document-service ./infra/helm-charts/document-service -n study-mate \
  --set image.tag=latest \
  --set postgres.host="study-mate-postgres" \
  --set postgres.port=5432 \
  --set postgres.database="study-mate" \
  --set postgres.username="postgres" \
  --set postgres.secretName="study-mate-postgres" \
  --set jwt.secretName="auth-secret" \
  --set authService.host="study-mate-auth-service" \
  --set authService.port=8086 \
  --set genaiService.host="study-mate-genai-service" \
  --set genaiService.port=8085
```

### Option 3: GitHub Actions Manual Trigger

1. Go to GitHub repository
2. Navigate to **Actions** tab
3. Select the workflow (e.g., "Deploy Auth Service")
4. Click **Run workflow**
5. Choose branch and optional image tag
6. Click **Run workflow**

## Service Dependencies

### Required Infrastructure (Deploy First)
```bash
# PostgreSQL
helm install postgres bitnami/postgresql -n study-mate \
  --set auth.postgresPassword="your-password" \
  --set auth.database="study-mate"

# Weaviate
helm upgrade --install weaviate ./infra/helm/templates/infrastructure/weaviate -n study-mate

# Secrets
kubectl apply -f infra/helm/templates/common/secrets.yaml -n study-mate
```

### Service Deployment Order
1. **Auth Service** (no dependencies)
2. **Document Service** (depends on Auth Service)
3. **GenAI Backend** (depends on Weaviate)
4. **GenAI Service** (depends on Auth, Document, GenAI Backend)
5. **Client** (depends on all services)

## Configuration

### Environment Variables
Each service chart has configurable environment variables in `values.yaml`:

```yaml
# Example: Auth Service
env:
  - name: SPRING_PROFILES_ACTIVE
    value: docker
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:postgresql://{{ .Values.postgres.host }}:{{ .Values.postgres.port }}/{{ .Values.postgres.database }}"
```

### Service Discovery
Services communicate using Kubernetes service names:
- Auth Service: `study-mate-auth-service:8086`
- Document Service: `study-mate-document-service:8084`
- GenAI Service: `study-mate-genai-service:8085`
- GenAI Backend: `study-mate-genai:8081`

## Monitoring and Troubleshooting

### Check Service Status
```bash
# List all releases
helm list -n study-mate

# Check specific service
helm status auth-service -n study-mate

# Check pods
kubectl get pods -n study-mate -l app.kubernetes.io/name=auth-service

# Check logs
kubectl logs -f deployment/auth-service -n study-mate
```

### Health Checks
Each service includes health check endpoints:
- Auth Service: `http://auth-service:8086/actuator/health`
- Document Service: `http://document-service:8084/actuator/health`
- GenAI Service: `http://genai-service:8085/actuator/health`
- GenAI Backend: `http://genai:8081/health`

### Port Forwarding for Testing
```bash
# Test auth service locally
kubectl port-forward svc/auth-service 8086:8086 -n study-mate

# Test document service locally
kubectl port-forward svc/document-service 8084:8084 -n study-mate
```

## Benefits of Modular Deployment

1. **Faster Deployments**: Only rebuild and deploy changed services
2. **Independent Scaling**: Scale services based on demand
3. **Easier Testing**: Test individual services in isolation
4. **Reduced Risk**: Changes to one service don't affect others
5. **Better CI/CD**: Parallel builds and deployments
6. **Resource Efficiency**: Only use resources for active services

## Migration from Monolithic Chart

To migrate from the current monolithic chart:

1. **Deploy infrastructure first**:
   ```bash
   helm upgrade --install study-mate ./infra/helm -n study-mate \
     --set authService.enabled=false \
     --set documentService.enabled=false \
     --set genaiService.enabled=false \
     --set client.enabled=false \
     --set genAi.enabled=false
   ```

2. **Deploy services individually**:
   ```bash
   # Deploy auth service
   helm upgrade --install auth-service ./infra/helm-charts/auth-service -n study-mate
   
   # Deploy document service
   helm upgrade --install document-service ./infra/helm-charts/document-service -n study-mate
   
   # Continue with other services...
   ```

3. **Remove services from monolithic chart**:
   ```bash
   helm upgrade study-mate ./infra/helm -n study-mate \
     --set authService.enabled=false \
     --set documentService.enabled=false \
     --set genaiService.enabled=false \
     --set client.enabled=false \
     --set genAi.enabled=false
   ```

## Troubleshooting

### Common Issues

1. **Service not starting**:
   ```bash
   kubectl describe pod <pod-name> -n study-mate
   kubectl logs <pod-name> -n study-mate
   ```

2. **Image pull errors**:
   ```bash
   # Check if image exists
   docker pull ghcr.io/aet-devops25/team-3/auth-service:latest
   
   # Check image pull secrets
   kubectl get secrets -n study-mate
   ```

3. **Service communication issues**:
   ```bash
   # Test service connectivity
   kubectl run test-pod --image=busybox -it --rm --restart=Never -n study-mate
   # Inside pod: wget -O- http://auth-service:8086/actuator/health
   ```

4. **Helm deployment failures**:
   ```bash
   # Check release status
   helm status auth-service -n study-mate
   
   # Rollback if needed
   helm rollback auth-service -n study-mate
   ```

### Getting Help

- Check GitHub Actions logs for build/deployment errors
- Review service logs: `kubectl logs -f deployment/<service-name> -n study-mate`
- Check Kubernetes events: `kubectl get events -n study-mate --sort-by='.lastTimestamp'`
- Verify secrets exist: `kubectl get secrets -n study-mate` 