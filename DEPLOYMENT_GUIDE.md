# StudyMate Deployment Guide

This guide covers all deployment methods for StudyMate, including CI/CD workflows and manual deployments.

## 🏗️ Architecture Overview

StudyMate supports multiple deployment environments:

- **🚀 EC2 Deployment** - AWS EC2 with Docker Compose + Traefik (Production)
- **☸️ Kubernetes Deployment** - Student Rancher cluster with Helm (Development/Staging)
- **🔧 Local Development** - Docker Compose for local development

## 📦 Container Images

StudyMate uses environment-specific container images:

| Environment | Image Tag | API Base URL | Usage |
|-------------|-----------|--------------|-------|
| EC2/AWS | `ec2-latest` | `https://studymate-tum.xyz` | Production on AWS |
| Kubernetes | `k8s-latest` | `https://studymate.student.k8s.aet.cit.tum.de` | Student cluster |
| Validation | `validation-*` | Default (window.location.origin) | CI/CD testing |

## 🔄 CI/CD Workflows

### 1. Build Images Workflow (`.github/workflows/build-images.yml`)

**Triggers:** Push to `main` branch or manual dispatch

**What it does:**
- Builds environment-specific client images
- Builds all microservice images
- Pushes to GitHub Container Registry

**Image Tags Created:**
- `ghcr.io/aet-devops25/team-3/client:ec2-{sha}` and `ec2-latest`
- `ghcr.io/aet-devops25/team-3/client:k8s-{sha}` and `k8s-latest`
- All other services with `{sha}` and `latest` tags

### 2. Deploy to AWS Workflow (`.github/workflows/deploy_aws.yml`)

**Triggers:** Push to `main` branch or manual dispatch

**What it does:**
- Builds EC2-specific images
- Deploys to AWS EC2 using Ansible
- Uses Docker Compose with Traefik

**Configuration:**
- Domain: `studymate-tum.xyz`
- Image: `ec2-latest`
- SSL: Let's Encrypt via Traefik

### 3. Deploy to Kubernetes Workflow (`.github/workflows/deploy-kubernetes.yml`)

**Triggers:** Push to `main` branch, path changes, or manual dispatch

**What it does:**
- Deploys to Student Rancher cluster
- Uses Helm charts
- Configures ingress and SSL

**Configuration:**
- Domain: `studymate.student.k8s.aet.cit.tum.de`
- Image: `k8s-latest`
- Namespace: `studymate`

### 4. Build Validation Workflow (`.github/workflows/build-validation.yml`)

**Triggers:** Pull requests

**What it does:**
- Validates that all components build successfully
- Runs tests
- Does not deploy

## 🚀 Manual Deployment Instructions

### EC2 Deployment (AWS)

1. **Prerequisites:**
   ```bash
   # EC2 instance with Docker and Docker Compose
   # Domain pointing to EC2 public IP
   ```

2. **Deploy:**
   ```bash
   # Set environment variables
   export IMAGE_TAG=ec2-latest
   export OPEN_WEBUI_API_KEY_CHAT="your-key"
   export OPEN_WEBUI_API_KEY_GEN="your-key"
   export LANGSMITH_API_KEY="your-key"
   
   # Run deployment
   ansible-playbook -i inventory.ini ansible/deploy.yml
   ```

3. **Access:**
   - URL: https://studymate-tum.xyz
   - Traefik Dashboard: https://studymate-tum.xyz:8080

### Kubernetes Deployment (Student Cluster)

1. **Prerequisites:**
   ```bash
   # kubectl configured for student cluster
   # Helm 3.x installed
   ```

2. **Deploy:**
   ```bash
   # Create namespace
   kubectl create namespace studymate
   
   # Install/upgrade with Helm
   helm upgrade --install studymate ./infra/helm -n studymate \
     --set client.image.tag="k8s-latest" \
     --set ingress.host="studymate.student.k8s.aet.cit.tum.de" \
     --set-string secrets.postgres.data.password="$(openssl rand -base64 32)" \
     --set-string secrets.auth.data.jwtSecret="$(openssl rand -base64 64)" \
     --set-string secrets.genai.data.openWebUiApiKeyChat="your-key" \
     --set-string secrets.genai.data.openWebUiApiKeyGen="your-key" \
     --set-string secrets.genai.data.langsmithApiKey="your-key"
   ```

3. **Access:**
   - URL: https://studymate.student.k8s.aet.cit.tum.de

### Local Development

1. **Start Development Environment:**
   ```bash
   ./start-dev
   ```

2. **Access:**
   - Frontend: http://localhost:3000
   - Backend: http://localhost:8082
   - GenAI: http://localhost:8081

## 🔧 Configuration Files

### Docker Compose Files

- `docker-compose.yml` - Local development
- `docker-compose.yml.j2` - EC2 deployment template (uses `ec2-{tag}` images)
- `docker-compose.microservices.yml` - Microservices development

### Helm Configuration

- `infra/helm/values.yaml` - Default Helm values (uses `k8s-latest` image)
- `infra/helm/templates/` - Kubernetes resource templates

### CI/CD Configuration

- `.github/workflows/` - GitHub Actions workflows
- `ansible/` - EC2 deployment automation

## 🔒 Secrets Management

### Required Secrets

| Secret | Description | Used In |
|--------|-------------|---------|
| `OPEN_WEBUI_API_KEY_CHAT` | OpenWebUI Chat API Key | All deployments |
| `OPEN_WEBUI_API_KEY_GEN` | OpenWebUI Generation API Key | All deployments |
| `LANGSMITH_API_KEY` | LangSmith API Key | All deployments |
| `GRAFANA_ADMIN_USERNAME` | Grafana admin username | EC2 only |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password | EC2 only |

### Setting Secrets

**GitHub Secrets:**
1. Go to repository Settings → Secrets and variables → Actions
2. Add the required secrets

**Kubernetes Secrets:**
```bash
# Automatically managed by Helm deployment
```

**Local Development:**
```bash
# Copy and edit environment file
cp genAi/example.env genAi/.env
# Edit the .env file with your keys
```

## 🐛 Troubleshooting

### Common Issues

1. **Image Pull Errors:**
   ```bash
   # Check if images exist
   docker pull ghcr.io/aet-devops25/team-3/client:k8s-latest
   ```

2. **Kubernetes Pod Failures:**
   ```bash
   # Check pod logs
   kubectl logs -f deployment/studymate-client -n studymate
   kubectl describe pod POD_NAME -n studymate
   ```

3. **EC2 Deployment Issues:**
   ```bash
   # Check Docker Compose logs
   docker-compose logs -f
   ```

### Monitoring

**Kubernetes:**
```bash
# Get all resources
kubectl get all -n studymate

# Check ingress
kubectl get ingress -n studymate
```

**EC2:**
```bash
# Check container status
docker ps

# View logs
docker-compose logs service-name
```

## 📝 Environment Variables

### Client Configuration

The client application uses environment-specific API base URLs:

- **EC2**: Built with `VITE_API_BASE_URL=https://studymate-tum.xyz`
- **Kubernetes**: Built with `VITE_API_BASE_URL=https://studymate.student.k8s.aet.cit.tum.de`
- **Development**: Uses `window.location.origin` (dynamic)

### Backend Configuration

All backend services use environment variables for:
- Database connections
- Service URLs
- API keys
- JWT secrets

## 🚦 Deployment Status

Check deployment status:

- **EC2**: https://studymate-tum.xyz
- **Kubernetes**: https://studymate.student.k8s.aet.cit.tum.de
- **GitHub Actions**: Repository → Actions tab

## 📞 Support

For deployment issues:
1. Check this documentation
2. Review CI/CD workflow logs
3. Check application logs
4. Verify configuration files 