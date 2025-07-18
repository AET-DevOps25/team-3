# Infrastructure Directory

This directory contains infrastructure configurations for the StudyMate application.

## 🎯 **Current Active Infrastructure: Kubernetes (Helm)**

The **primary deployment target** is **Kubernetes** using the TUM Student Rancher cluster.

### ✅ **Active Components:**
- `helm/` - **Kubernetes deployment** (currently used)
- GitHub Actions workflow for automated deployment

### ⚠️ **Legacy Components:**
- `aws/` - **AWS EC2 infrastructure** (legacy, not currently used)

## 🚀 **Deployment Flow**

1. **GitHub Actions** triggers on push to `main`
2. **Deploys Bitnami PostgreSQL** to Kubernetes
3. **Deploys StudyMate application** using Helm chart
4. **Application runs on** `studymate.student.k8s.aet.cit.tum.de`

## 📁 **Directory Structure**

```
infra/
├── helm/                    # ✅ ACTIVE: Kubernetes deployment
│   ├── Chart.yaml          # Helm chart metadata
│   ├── values.yaml         # Application configuration
│   ├── templates/          # Kubernetes manifests
│   └── README.md           # Deployment guide
└── aws/                    # ⚠️ LEGACY: AWS EC2 (not used)
    ├── terraform/          # Terraform configuration
    └── ansible/            # Ansible playbooks
```

## 🔧 **Configuration**

### Environment Variables (GitHub Actions)
- `HELM_NAMESPACE`: `studymate`
- `DOMAIN`: Configurable via workflow input
- `POSTGRES_PERSISTENCE_SIZE`: `8Gi`
- `WEAVIATE_PERSISTENCE_SIZE`: `10Gi`

### Secrets Required
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `OPEN_WEBUI_API_KEY_CHAT`
- `OPEN_WEBUI_API_KEY_GEN`
- `LANGSMITH_API_KEY`

## 🧹 **Cleanup Recommendations**

1. **Remove AWS infrastructure** if not needed
2. **Update documentation** to reflect Kubernetes-only deployment
3. **Consider archiving** AWS configurations if they might be needed later

## 📞 **Support**

For deployment issues, check:
1. GitHub Actions workflow logs
2. Kubernetes pod status: `kubectl get pods -n studymate`
3. Helm chart values: `infra/helm/values.yaml` 