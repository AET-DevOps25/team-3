# 🔐 StudyMate Secrets Management Guide

This guide covers secure secrets management for StudyMate application deployment.

## 🚨 Current Security Status

**✅ Good practices in place:**
- No secrets committed to version control
- Placeholder values with validation
- Kubernetes-native secret management
- Helm template validation for required secrets

## 🔑 Required Secrets

| Secret Name | Purpose | Example Format |
|-------------|---------|----------------|
| `POSTGRES_PASSWORD` | Database password | `mySecureDBPassword123!` |
| `JWT_SECRET` | JWT token signing | `256-bit-base64-encoded-key` |
| `OPEN_WEBUI_API_KEY_CHAT` | Chat service key | `your-chat-api-key` |
| `OPEN_WEBUI_API_KEY_GEN` | Generation service key | `your-generation-api-key` |

## 🎯 Deployment Options

> **💡 Recommendation:** For most teams, GitHub Secrets provide sufficient security and are much simpler than external secrets managers. Only consider external systems for enterprise-scale deployments with compliance requirements.

### Option 1: GitHub Actions (Recommended)

#### Step 1: Add Secrets to GitHub
1. Go to your repository → **Settings** → **Secrets and variables** → **Actions**
2. Add these repository secrets:

```bash
Name: POSTGRES_PASSWORD
Value: your-strong-postgres-password

Name: JWT_SECRET  
Value: your-256-bit-jwt-signing-key

Name: OPEN_WEBUI_API_KEY_CHAT
Value: your-chat-api-key

Name: OPEN_WEBUI_API_KEY_GEN
Value: your-generation-api-key
```

#### Step 2: GitHub Actions Workflow
```yaml
name: Deploy to Kubernetes

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Deploy to Kubernetes
        env:
          POSTGRES_PASSWORD: ${{ secrets.POSTGRES_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
          OPEN_WEBUI_API_KEY_CHAT: ${{ secrets.OPEN_WEBUI_API_KEY_CHAT }}
          OPEN_WEBUI_API_KEY_GEN: ${{ secrets.OPEN_WEBUI_API_KEY_GEN }}
        run: |
          ./deploy-k8s.sh --env prod --domain yourdomain.com
```

### Option 2: Local Development

#### Environment Variables
```bash
# Set secrets as environment variables
export POSTGRES_PASSWORD="your-local-dev-password"
export JWT_SECRET="your-dev-jwt-secret"
export OPEN_WEBUI_API_KEY_CHAT="your-chat-key"
export OPEN_WEBUI_API_KEY_GEN="your-gen-key"

# Deploy
./deploy-k8s.sh --env local
```

#### `.env` File (Local Only - Never Commit!)
```bash
# Create .env file for local development only
# Add .env to .gitignore!

POSTGRES_PASSWORD=local-dev-password
JWT_SECRET=local-jwt-secret-key
OPEN_WEBUI_API_KEY_CHAT=local-chat-key
OPEN_WEBUI_API_KEY_GEN=local-gen-key
```

### Option 3: External Secret Management (Enterprise Only)

> **⚠️ Advanced:** Only needed for enterprise environments with compliance requirements, secret rotation, or multi-cloud deployments. Most teams should use GitHub Secrets instead.

#### AWS Secrets Manager
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
```

#### Azure Key Vault
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: azure-key-vault
spec:
  provider:
    azurekv:
      vaultUrl: "https://my-vault.vault.azure.net/"
```

## 🛡️ Security Best Practices

### ✅ DO
- Use strong, unique passwords (20+ characters)
- Rotate secrets regularly
- Use different secrets for different environments
- Store secrets in encrypted secret management systems
- Limit access to secrets on need-to-know basis
- Use Kubernetes RBAC to restrict secret access

### ❌ DON'T
- Commit secrets to version control
- Share secrets via email/chat
- Use production secrets in development
- Log secret values
- Store secrets in plain text files

## 🔧 Secret Generation

### Strong Password Generation
```bash
# Generate secure PostgreSQL password
openssl rand -base64 32

# Generate JWT secret (256-bit)
openssl rand -base64 32

# Or use password managers like 1Password, Bitwarden
```

### JWT Secret Requirements
- Minimum 256 bits (32 bytes)
- Use cryptographically secure random generation
- Base64 encoded for easy handling

## 🚨 Secret Rotation

### PostgreSQL Password
1. Generate new password
2. Update secret in management system
3. Deploy with new secret
4. Verify connectivity
5. Remove old password access

### JWT Secret
1. Generate new secret
2. Deploy with new secret
3. Existing tokens remain valid until expiry
4. Monitor for authentication issues

## 🔍 Troubleshooting

### Common Issues

**Missing Secrets Error**
```bash
[ERROR] Missing required secrets: POSTGRES_PASSWORD JWT_SECRET
```
**Solution:** Set all required environment variables

**Invalid Secret Format**
```bash
Error: JWT secret must be at least 256 bits
```
**Solution:** Generate longer secret using secure method

**Secret Not Found in Kubernetes**
```bash
Error: secret "postgres-secret" not found
```
**Solution:** Verify secrets are created during deployment

### Validation Commands
```bash
# Check if secrets exist in Kubernetes
kubectl get secrets -n team-3

# View secret metadata (not values)
kubectl describe secret postgres-secret -n team-3

# Test deployment with dry-run
./deploy-k8s.sh --dry-run --env local
```

## 📚 Additional Resources

- [Kubernetes Secrets Documentation](https://kubernetes.io/docs/concepts/configuration/secret/)
- [GitHub Actions Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [External Secrets Operator](https://external-secrets.io/)
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html) 

## **🔍 Current State Analysis:**

**✅ LangSmith is configured in:**
- `docker-compose.yml.j2` (genai service)
- GitHub Actions workflows  
- Ansible deployment

**❌ LangSmith is missing from:**
- Kubernetes Helm templates
- `values.yaml` 
- `deploy-k8s.sh` validation

## **🛠️ Required Changes:**

### **1. Add LangSmith Secret to `infra/helm/templates/common/secrets.yaml`**

Add this to the genai secret data section (around line 68):

```yaml
# Add after line 68 in secrets.yaml:
data:
  open-webui-api-key-chat: {{ .Values.secrets.genai.data.openWebUiApiKeyChat | b64enc }}
  open-webui-api-key-gen: {{ .Values.secrets.genai.data.openWebUiApiKeyGen | b64enc }}
  {{- if .Values.secrets.genai.data.langsmithApiKey }}
  langsmith-api-key: {{ .Values.secrets.genai.data.langsmithApiKey | b64enc }}
  {{- end }}
```

### **2. Add LangSmith Environment Variables to `infra/helm/values.yaml`**

Add these to the genAi env section (around line 143):

```yaml
# Add after the WEAVIATE_PORT line in genAi.env:
    - name: WEAVIATE_PORT
      value: "8083"

    # LangSmith Configuration
    - name: LANGSMITH_TRACING
      value: "true"
    - name: LANGSMITH_ENDPOINT
      value: "https://api.smith.langchain.com"
    - name: LANGSMITH_PROJECT
      value: "studymate"
    - name: LANGSMITH_API_KEY
      valueFrom:
        secretKeyRef:
          name: genai-secret
          key: langsmith-api-key
```

### **3. Add LangSmith Secret Placeholder to `values.yaml` secrets section**

Add this to the genai secrets data section (around line 248):

```yaml
# Add to genai.data section:
  genai:
    create: true
    name: genai-secret
    existingSecret: ""
    data:
      openWebUiApiKeyChat: "placeholder-chat-api-key"
      openWebUiApiKeyGen: "placeholder-gen-api-key"
      langsmithApiKey: "placeholder-langsmith-api-key"  # OPTIONAL: Set via GitHub secret LANGSMITH_API_KEY
```

### **4. Update `deploy-k8s.sh` Secret Validation**

Add this to the check_secrets() function (around line 50):

```bash
# Add to deploy-k8s.sh check_secrets() function:
    if [ -z "$LANGSMITH_API_KEY" ]; then
        echo "⚠️  LANGSMITH_API_KEY not set (optional for LangSmith tracing)"
    fi
```

### **5. Update Helm Command in `deploy-k8s.sh`**

Add this to the helm upgrade command (around line 280):

```bash
# Add to the helm upgrade command in deploy-k8s.sh:
        --set-string secrets.genai.data.langsmithApiKey="$LANGSMITH_API_KEY" \
```

### **6. Add to GitHub Actions Secrets**

Add `LANGSMITH_API_KEY` to your GitHub repository secrets:
1. Go to your repo → Settings → Secrets and variables → Actions
2. Add new secret: `LANGSMITH_API_KEY` with your actual LangSmith API key

### **7. Update `.github/workflows/deploy-k8s.yml`**

Add the environment variable (around line 58):

```yaml
# Add to the Deploy to Kubernetes step env section:
        env:
          POSTGRES_PASSWORD: ${{ secrets.POSTGRES_PASSWORD }}
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
          OPEN_WEBUI_API_KEY_CHAT: ${{ secrets.OPEN_WEBUI_API_KEY_CHAT }}
          OPEN_WEBUI_API_KEY_GEN: ${{ secrets.OPEN_WEBUI_API_KEY_GEN }}
          LANGSMITH_API_KEY: ${{ secrets.LANGSMITH_API_KEY }}
```

## **🎯 Summary:**

**Current secrets (required):**
- `POSTGRES_PASSWORD`
- `JWT_SECRET` 
- `OPEN_WEBUI_API_KEY_CHAT`
- `OPEN_WEBUI_API_KEY_GEN`

**New secret (optional):**
- `LANGSMITH_API_KEY` - for LangSmith tracing/monitoring

**After these changes:**
- ✅ LangSmith will work in Kubernetes like it does in Docker Compose
- ✅ Optional - won't break deployment if not provided
- ✅ Full tracing and monitoring via LangSmith dashboard

**Test the changes:**
```bash
# Add LANGSMITH_API_KEY to your GitHub secrets, then:
./deploy-k8s.sh --env dev --domain your-domain.com
```

The LangSmith integration will provide valuable insights into your AI/GenAI service performance and usage patterns! 🚀 