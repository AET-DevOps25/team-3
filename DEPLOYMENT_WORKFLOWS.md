# 🚀 Deployment Workflows Guide

This document explains the GitHub Actions workflows for deploying StudyMate to Kubernetes.

## 📋 Overview

The deployment system supports **push to main** and **pull request** triggers, providing:
- ✅ **Automatic deployments** on push to main branch
- ✅ **Individual service deployments** for targeted updates
- ✅ **Hardcoded kubeconfig** for simplified setup

## 🔄 Workflow Triggers

### **Main Deployment Workflows**

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `deploy-kubernetes.yml` | Push to main + PR | Full application deployment (includes ingress) |
| `deploy-auth-service.yml` | Push to main + PR | Auth service only |
| `deploy-client.yml` | Push to main + PR | Client (React) only |
| `deploy-document-service.yml` | Push to main + PR | Document service only |
| `deploy-genai-service.yml` | Push to main + PR | GenAI service only |
| `deploy-genai-backend.yml` | Push to main + PR | GenAI backend only |
| `deploy-infrastructure.yml` | Push to main + PR | PostgreSQL + Weaviate |

## 🎯 Trigger Details

### **Push to Main Branch**
```yaml
on:
  push:
    branches: [main]
    paths:
      - 'server/**'          # Any server changes
      - 'client/**'          # Any client changes
      - 'genai/**'           # Any GenAI changes
      - 'infra/helm/**'      # Any Helm chart changes
      - '.github/workflows/**' # Workflow changes
```

### **Pull Request Events**
```yaml
on:
  pull_request:
    types: [opened, synchronize, reopened]
    paths:
      - 'server/**'          # Server changes in PR
      - 'client/**'          # Client changes in PR
      - 'genai/**'           # GenAI changes in PR
      - 'infra/helm/**'      # Helm chart changes in PR
      - '.github/workflows/**' # Workflow changes in PR
```

## 🚀 Deployment Scenarios

### **1. Push to Main Branch**
- **Trigger**: Any push to `main` branch
- **Action**: Deploy to production namespace (`study-mate`)
- **Result**: Live application updated (includes ingress configuration)

### **2. Individual Service PR**
- **Trigger**: PR with changes to specific service
- **Action**: Deploy only that service to production
- **Result**: Targeted service update

### **3. Ingress Management**
- **Note**: Ingress is managed by the main `deploy-kubernetes.yml` workflow
- **No separate ingress workflow**: Ingress configuration is included in the main Helm chart
- **Automatic updates**: Ingress is updated whenever the main deployment runs

## 📦 Namespace Strategy

### **Production Namespace**
- **Name**: `study-mate`
- **Used for**: All deployments (main branch and PRs)
- **Persistence**: Long-term data storage
- **Access**: Public via ingress

## 🔐 Secrets Required

All workflows require these GitHub secrets:

| Secret | Purpose | Example |
|--------|---------|---------|
| `POSTGRES_PASSWORD` | Database password | `mySecureDBPassword123!` |
| `JWT_SECRET` | JWT token signing | `256-bit-base64-encoded-key` |
| `OPEN_WEBUI_API_KEY_CHAT` | Chat service key | `your-chat-api-key` |
| `OPEN_WEBUI_API_KEY_GEN` | Generation service key | `your-generation-api-key` |
| `LANGSMITH_API_KEY` | LangSmith API key | `lsv2_pt_your_key` (optional) |

## 🔧 Kubernetes Configuration

All workflows use a standardized hardcoded kubeconfig approach for the student cluster:

### **Standardized Deployment Pattern:**
All workflows use the same Helm deployment format:

```yaml
- name: Deploy Helm Chart
  env:
    HELM_RELEASE_NAME: {service-name}
    CHART_PATH: ./infra/helm-charts/{service-name}
    HELM_NAMESPACE: study-mate
    IMAGE_TAG: ${{ github.event.inputs.image_tag || github.sha }}
  run: |
    helm upgrade --install ${{ env.HELM_RELEASE_NAME }} ${{ env.CHART_PATH }} \
      --namespace ${{ env.HELM_NAMESPACE }} \
      --set image.tag=${{ env.IMAGE_TAG }} \
      --wait --timeout=5m
```

### **Kubeconfig Setup:**
Each workflow includes a "Configure kubectl" step that creates the kubeconfig file:

```yaml
- name: Configure kubectl
  run: |
    mkdir -p $HOME/.kube
    cat > $HOME/.kube/config << 'EOF'
    apiVersion: v1
    kind: Config
    clusters:
    - name: "student"
      cluster:
        server: "https://rancher.ase.cit.tum.de/k8s/clusters/c-m-nhcfjg9h"
    
    users:
    - name: "student"
      user:
        token: "kubeconfig-u-g7fbq4tzcsrjvb2:dtw5qr2nkwl5hl4r676dlmt7v9lh9bw5xgkp5l65pf6tr6ql79zsmm"
    
    contexts:
    - name: "student"
      context:
        user: "student"
        cluster: "student"
    
    current-context: "student"
    EOF
    chmod 600 $HOME/.kube/config
```

### **Benefits:**
- ✅ **No secrets management** - kubeconfig is hardcoded in workflows
- ✅ **Consistent approach** - all workflows use the same pattern
- ✅ **Simplified setup** - no need to manage base64-encoded secrets
- ✅ **Easy updates** - change token in one place, update all workflows
- ✅ **Standardized deployment** - all services use the same Helm deployment format
- ✅ **Permission-aware** - no cluster connection tests that require elevated permissions

## 🎯 Usage Examples

### **Deploy Single Service**
```bash
# Make changes to auth service
git add server/auth-service/
git commit -m "Update auth service"
git push origin feature/auth-update

# Create PR - triggers auth service deployment
```

### **Deploy Full Application**
```bash
# Make changes to multiple components
git add server/ client/ infra/helm-charts/
git commit -m "Major application update"
git push origin feature/major-update

# Create PR - triggers full deployment
```

### **Manual Deployment**
```bash
# Go to GitHub Actions tab
# Select workflow (e.g., "Deploy Auth Service")
# Click "Run workflow"
# Fill in parameters
# Click "Run workflow"
```

## 🔍 Monitoring & Debugging

### **Check Deployment Status**
```bash
# Production
kubectl get pods -n study-mate
```

### **View Logs**
```bash
# Production
kubectl logs -f deployment/auth-service -n study-mate
```

### **Port Forward for Local Access**
```bash
# Production
kubectl port-forward svc/client 8080:80 -n study-mate
```

## 🚨 Troubleshooting

### **Common Issues**

**1. Workflow Not Triggering**
- Check if file paths match trigger conditions
- Verify branch name is correct
- Ensure changes are in monitored directories

**2. Deployment Fails**
- Check GitHub secrets are set correctly
- Verify Kubernetes cluster access
- Review workflow logs for specific errors

**3. Namespace Issues**
- Ensure `study-mate` namespace exists
- Create manually if needed: `kubectl create namespace study-mate`

### **Useful Commands**
```bash
# Check all namespaces
kubectl get namespaces

# Check all pods across namespaces
kubectl get pods --all-namespaces

# Check Helm releases
helm list --all-namespaces

# Check events
kubectl get events --all-namespaces --sort-by='.lastTimestamp'
```

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [StudyMate Helm Charts](../infra/helm-charts/) 