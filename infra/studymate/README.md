# StudyMate Kubernetes Deployment Guide

This guide covers how to deploy and manage the StudyMate application on Kubernetes using Helm.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Secret Management](#secret-management)
- [Configuration](#configuration)
- [Deployment Examples](#deployment-examples)
- [Monitoring & Troubleshooting](#monitoring--troubleshooting)
- [Advanced Usage](#advanced-usage)

## Prerequisites

### Required Tools

- **Kubernetes cluster** (1.19+)
- **Helm** (3.0+)
- **kubectl** configured with cluster access
- **OpenSSL** (for generating secrets)

### Cluster Requirements

- **Storage**: Default StorageClass configured
- **Ingress**: Nginx Ingress Controller (optional)
- **Cert-Manager**: For TLS certificates (optional)

### Verify Prerequisites

```bash
# Check Kubernetes connection
kubectl get nodes

# Check Helm version
helm version

# Check storage class
kubectl get storageclass

# Check available resources
kubectl describe nodes
```

## Quick Start

### 1. Clone Repository

```bash
git clone <repository-url>
cd team-3/infra/studymate
```

### 2. Create Namespace

```bash
kubectl create namespace team-3
```

### 3. Generate Secure Secrets

```bash
# Generate PostgreSQL password
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# Generate JWT secret
JWT_SECRET=$(openssl rand -base64 64)

# Your API keys (replace with actual values)
OPENAI_API_KEY="sk-your-openai-api-key"
OPEN_WEBUI_API_KEY_CHAT="sk-your-chat-api-key"
OPEN_WEBUI_API_KEY_GEN="sk-your-gen-api-key"

# Generate database URL
DATABASE_URL="postgresql://postgres:${POSTGRES_PASSWORD}@studymate-postgres:5432/mydb"

echo "Generated secrets:"
echo "  PostgreSQL Password: ${POSTGRES_PASSWORD}"
echo "  JWT Secret: ${JWT_SECRET}"
echo "  Database URL: ${DATABASE_URL}"
```

### 4. Deploy Application

```bash
helm install studymate . -n team-3 \
  --set-string secrets.postgres.data.password="${POSTGRES_PASSWORD}" \
  --set-string secrets.genai.data.apiKey="${OPENAI_API_KEY}" \
  --set-string secrets.genai.data.openWebUiApiKeyChat="${OPEN_WEBUI_API_KEY_CHAT}" \
  --set-string secrets.genai.data.openWebUiApiKeyGen="${OPEN_WEBUI_API_KEY_GEN}" \
  --set-string secrets.server.data.jwtSecret="${JWT_SECRET}" \
  --set-string secrets.server.data.databaseUrl="${DATABASE_URL}"
```

### 5. Verify Deployment

```bash
# Check pod status
kubectl get pods -n team-3

# Check services
kubectl get svc -n team-3

# Check persistent volumes
kubectl get pvc -n team-3
```

## Secret Management

### Required Secrets

The application requires the following secrets:

| Secret | Key | Description | Example |
|--------|-----|-------------|---------|
| `postgres-secret` | `username` | PostgreSQL username | `postgres` |
| `postgres-secret` | `password` | PostgreSQL password | `<32-char-random>` |
| `genai-secret` | `api-key` | OpenAI API key | `sk-...` |
| `genai-secret` | `open-webui-api-key-chat` | OpenWebUI Chat API key | `sk-...` |
| `genai-secret` | `open-webui-api-key-gen` | OpenWebUI Gen API key | `sk-...` |
| `server-secret` | `jwt-secret` | JWT signing secret | `<64-char-random>` |
| `server-secret` | `database-url` | Database connection URL | `postgresql://...` |

### Security Best Practices

1. **Never commit secrets to version control**
2. **Use strong, randomly generated passwords**
3. **Rotate secrets regularly**
4. **Use external secret management in production**
5. **Monitor secret access and usage**

### Secret Generation Commands

```bash
# PostgreSQL password (32 characters)
openssl rand -base64 32

# JWT secret (64 characters)  
openssl rand -base64 64

# Verify secret strength
echo "Password entropy: $(openssl rand -base64 32 | wc -c) characters"
```

## Configuration

### Default Values

The following services are deployed by default:

- **Client**: React frontend (Port 80)
- **Server**: Spring Boot API (Port 8082)
- **GenAI**: AI/ML service (Port 8081)
- **PostgreSQL**: Database (Port 5432)
- **Weaviate**: Vector database (Port 8083)

### Storage Configuration

```yaml
# Default storage settings
postgres:
  persistence:
    size: 8Gi
    storageClass: ""  # Uses default storage class

weaviate:
  persistence:
    size: 10Gi
    storageClass: ""  # Uses default storage class
```

### Customizing Resources

```bash
# Deploy with custom resource limits
helm install studymate . -n team-3 \
  --set postgres.persistence.size=20Gi \
  --set weaviate.persistence.size=50Gi \
  --set-string secrets.postgres.data.password="..." \
  # ... other secrets
```

## Deployment Examples

### Development Environment

```bash
# Minimal resources for development
helm install studymate . -n team-3 \
  --set postgres.persistence.size=2Gi \
  --set weaviate.persistence.size=5Gi \
  --set-string secrets.postgres.data.password="dev-password" \
  --set-string secrets.genai.data.apiKey="sk-dev-key" \
  --set-string secrets.genai.data.openWebUiApiKeyChat="sk-dev-chat" \
  --set-string secrets.genai.data.openWebUiApiKeyGen="sk-dev-gen" \
  --set-string secrets.server.data.jwtSecret="dev-jwt-secret" \
  --set-string secrets.server.data.databaseUrl="postgresql://postgres:dev-password@studymate-postgres:5432/mydb"
```

### Production Environment

```bash
# Production deployment with external secrets
helm install studymate . -n team-3 \
  --set postgres.persistence.size=100Gi \
  --set weaviate.persistence.size=200Gi \
  --set postgres.persistence.storageClass="fast-ssd" \
  --set weaviate.persistence.storageClass="fast-ssd" \
  --set secrets.postgres.existingSecret="prod-postgres-secret" \
  --set secrets.genai.existingSecret="prod-genai-secret" \
  --set secrets.server.existingSecret="prod-server-secret"
```

### Using External Secrets

```bash
# First create external secrets
kubectl create secret generic prod-postgres-secret \
  --from-literal=username=postgres \
  --from-literal=password="$(openssl rand -base64 32)" \
  -n team-3

kubectl create secret generic prod-genai-secret \
  --from-literal=api-key="sk-your-openai-key" \
  --from-literal=open-webui-api-key-chat="sk-your-chat-key" \
  --from-literal=open-webui-api-key-gen="sk-your-gen-key" \
  -n team-3

kubectl create secret generic prod-server-secret \
  --from-literal=jwt-secret="$(openssl rand -base64 64)" \
  --from-literal=database-url="postgresql://postgres:password@studymate-postgres:5432/mydb" \
  -n team-3

# Deploy using external secrets
helm install studymate . -n team-3 \
  --set secrets.postgres.existingSecret="prod-postgres-secret" \
  --set secrets.genai.existingSecret="prod-genai-secret" \
  --set secrets.server.existingSecret="prod-server-secret"
```

## Monitoring & Troubleshooting

### Health Checks

```bash
# Check pod status
kubectl get pods -n team-3

# Check pod logs
kubectl logs -l app.kubernetes.io/component=postgres -n team-3
kubectl logs -l app.kubernetes.io/component=genai -n team-3
kubectl logs -l app.kubernetes.io/component=server -n team-3

# Check service connectivity
kubectl get svc -n team-3
```

### Data Persistence Verification

```bash
# Check persistent volumes
kubectl get pvc -n team-3
kubectl get pv

# Test data persistence
kubectl exec -it deployment/studymate-postgres -n team-3 -- \
  psql -U postgres -d mydb -c "SELECT version();"
```

### Common Issues

#### 1. Secret Validation Errors

**Error**: `ERROR: PostgreSQL password is required`

**Solution**: Ensure all required secrets are provided
```bash
helm install studymate . -n team-3 \
  --set-string secrets.postgres.data.password="your-password" \
  # ... other required secrets
```

#### 2. Storage Issues

**Error**: `PVC stuck in Pending state`

**Solution**: Check storage class availability
```bash
kubectl get storageclass
kubectl describe pvc studymate-postgres-pvc -n team-3
```

#### 3. Pod Startup Issues

**Error**: `CrashLoopBackOff`

**Solution**: Check pod logs and resource limits
```bash
kubectl logs deployment/studymate-postgres -n team-3
kubectl describe pod -l app.kubernetes.io/component=postgres -n team-3
```

### Debugging Commands

```bash
# Get all resources
kubectl get all -n team-3

# Check events
kubectl get events -n team-3 --sort-by=.metadata.creationTimestamp

# Check resource usage
kubectl top pods -n team-3

# Port forward for local access
kubectl port-forward svc/studymate-client 8080:80 -n team-3
```

## Advanced Usage

### Upgrading Deployment

```bash
# Upgrade with new values
helm upgrade studymate . -n team-3 \
  --set postgres.persistence.size=16Gi \
  --reuse-values

# Check rollout status
kubectl rollout status deployment/studymate-postgres -n team-3
```

### Backup and Recovery

```bash
# Create database backup
kubectl exec deployment/studymate-postgres -n team-3 -- \
  pg_dump -U postgres -d mydb > backup.sql

# Restore from backup
kubectl exec -i deployment/studymate-postgres -n team-3 -- \
  psql -U postgres -d mydb < backup.sql
```

### Scaling Services

```bash
# Scale specific services
helm upgrade studymate . -n team-3 \
  --set server.replicaCount=3 \
  --set genAi.replicaCount=2 \
  --reuse-values
```

### Custom Values File

Create a `custom-values.yaml`:

```yaml
# Custom configuration
postgres:
  persistence:
    size: 50Gi
    storageClass: "premium-ssd"

weaviate:
  persistence:
    size: 100Gi
    storageClass: "premium-ssd"

server:
  replicaCount: 2

genAi:
  replicaCount: 3

secrets:
  postgres:
    existingSecret: "external-postgres-secret"
  genai:
    existingSecret: "external-genai-secret"
  server:
    existingSecret: "external-server-secret"
```

Deploy with custom values:
```bash
helm install studymate . -n team-3 -f custom-values.yaml
```

## Uninstalling

```bash
# Remove deployment (keeps PVCs)
helm uninstall studymate -n team-3

# Remove persistent data (WARNING: This deletes all data!)
kubectl delete pvc -l app.kubernetes.io/instance=studymate -n team-3

# Remove namespace
kubectl delete namespace team-3
```

## Support

For issues and questions:
1. Check the [Troubleshooting](#monitoring--troubleshooting) section
2. Review pod logs: `kubectl logs -l app.kubernetes.io/name=studymate -n team-3`
3. Check cluster resources: `kubectl get events -n team-3`

---

**Security Note**: Always use strong, randomly generated secrets in production environments. Never commit actual secret values to version control.