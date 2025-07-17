#!/bin/bash

# StudyMate Kubernetes Deployment Script
# This script deploys the StudyMate application to Kubernetes using Helm

set -e

echo "🚀 Deploying StudyMate to Kubernetes..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check required secrets
check_secrets() {
    print_status "Validating required secrets..."
    
    local missing_secrets=()
    
    # Check for required secrets
    # POSTGRES_PASSWORD and JWT_SECRET are hardcoded in values.yaml
    
    if [ -z "$OPEN_WEBUI_API_KEY_CHAT" ]; then
        missing_secrets+=("OPEN_WEBUI_API_KEY_CHAT")
    fi
    
    if [ -z "$OPEN_WEBUI_API_KEY_GEN" ]; then
        missing_secrets+=("OPEN_WEBUI_API_KEY_GEN")
    fi
    
    # Optional LangSmith API key check
    if [ -z "$LANGSMITH_API_KEY" ]; then
        echo "⚠️  LANGSMITH_API_KEY not set (optional for LangSmith tracing)"
    fi
    
    if [ ${#missing_secrets[@]} -ne 0 ]; then
        print_error "Missing required secrets: ${missing_secrets[*]}"
        echo ""
        echo "Set them as environment variables:"
        for secret in "${missing_secrets[@]}"; do
            echo "  export $secret='your-secret-value'"
        done
        echo ""
        echo "Or in GitHub Actions, ensure these secrets are set in repository settings."
        exit 1
    fi
    
    print_success "All required secrets are present"
}

# Configuration
NAMESPACE="team-3"
RELEASE_NAME="studymate"
CHART_PATH="./infra/helm"

# Parse command line arguments
ENVIRONMENT="local"
DOMAIN="studymate.local"
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --domain)
            DOMAIN="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --env ENV        Environment (local/dev/prod) [default: local]"
            echo "  --domain DOMAIN  Domain name [default: studymate.local]"
            echo "  --dry-run        Perform a dry run without applying changes"
            echo "  --help           Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

print_status "Environment: $ENVIRONMENT"
print_status "Domain: $DOMAIN"
print_status "Namespace: $NAMESPACE"
print_status "Release: $RELEASE_NAME"

# Check secrets before proceeding
check_secrets

# Check prerequisites
print_status "Checking prerequisites..."

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed or not in PATH"
    exit 1
fi

# Check if helm is available
if ! command -v helm &> /dev/null; then
    print_error "helm is not installed or not in PATH"
    exit 1
fi

# Check if connected to cluster
if ! kubectl cluster-info &> /dev/null; then
    print_error "Not connected to a Kubernetes cluster"
    exit 1
fi

print_success "Prerequisites check passed"

# Check if Helm chart exists
if [ ! -f "$CHART_PATH/Chart.yaml" ]; then
    print_error "Helm chart not found at $CHART_PATH"
    exit 1
fi

# Create values override file based on environment
VALUES_FILE="values-${ENVIRONMENT}.yaml"
cat > "$VALUES_FILE" << EOF
# Generated values for $ENVIRONMENT environment
teamid: $NAMESPACE

ingress:
  enabled: true
  className: "nginx"
  host: "$DOMAIN"
  tls: true

# Image tags - update these for production deployments
authService:
  image:
    tag: "latest"
documentService:
  image:
    tag: "latest"
genaiService:
  image:
    tag: "latest"

client:
  image:
    tag: "latest"
genAi:
  image:
    tag: "latest"

# Secrets - IMPORTANT: Override these with actual values in production
secrets:
  postgres:
    data:
      username: postgres
      password: "changeme-postgres-password"
  auth:
    data:
      jwtSecret: "changeme-very-long-jwt-secret-key-at-least-256-bits"
  genai:
    data:
      apiKey: "changeme-openai-api-key"
      openWebUiApiKeyChat: "changeme-chat-api-key"
      openWebUiApiKeyGen: "changeme-gen-api-key"

EOF

if [ "$ENVIRONMENT" = "local" ]; then
    cat >> "$VALUES_FILE" << EOF

# Local development overrides
ingress:
  annotations:
    # For local development, disable HTTPS redirect
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
EOF
fi

print_success "Generated values file: $VALUES_FILE"

# Build Helm dependencies if needed
if [ -f "$CHART_PATH/Chart.lock" ]; then
    print_status "Updating Helm dependencies..."
    helm dependency update "$CHART_PATH"
fi

# Validate Helm chart
print_status "Validating Helm chart..."
if ! helm lint "$CHART_PATH" -f "$VALUES_FILE"; then
    print_error "Helm chart validation failed"
    exit 1
fi

print_success "Helm chart validation passed"

# Perform dry run if requested
if [ "$DRY_RUN" = true ]; then
    print_status "Performing dry run..."
    print_warning "Using placeholder values for secrets in dry-run mode"
    helm template "$RELEASE_NAME" "$CHART_PATH" \
        -f "$VALUES_FILE" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        --set-string secrets.postgres.data.password="placeholder-password" \
        --set-string secrets.auth.data.jwtSecret="placeholder-jwt-secret" \
        --set-string secrets.genai.data.openWebUiApiKeyChat="placeholder-chat-key" \
        --set-string secrets.genai.data.openWebUiApiKeyGen="placeholder-gen-key" > /tmp/studymate-dry-run.yaml
    
    print_success "Dry run completed. Templates saved to /tmp/studymate-dry-run.yaml"
    echo "📄 Generated $(grep -c '^---' /tmp/studymate-dry-run.yaml) Kubernetes resources"
    exit 0
fi

# Apply the deployment
print_status "Deploying to Kubernetes..."

# Check if release exists
if helm list -n "$NAMESPACE" | grep -q "$RELEASE_NAME"; then
    print_status "Upgrading existing release..."
    helm upgrade "$RELEASE_NAME" "$CHART_PATH" \
        -f "$VALUES_FILE" \
        --namespace "$NAMESPACE" \
        --set-string secrets.genai.data.openWebUiApiKeyChat="$OPEN_WEBUI_API_KEY_CHAT" \
        --set-string secrets.genai.data.openWebUiApiKeyGen="$OPEN_WEBUI_API_KEY_GEN" \
        --set-string secrets.genai.data.langsmithApiKey="$LANGSMITH_API_KEY" \
        --wait \
        --timeout=10m
else
    print_status "Installing new release..."
    helm install "$RELEASE_NAME" "$CHART_PATH" \
        -f "$VALUES_FILE" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        --set-string secrets.genai.data.openWebUiApiKeyChat="$OPEN_WEBUI_API_KEY_CHAT" \
        --set-string secrets.genai.data.openWebUiApiKeyGen="$OPEN_WEBUI_API_KEY_GEN" \
        --set-string secrets.genai.data.langsmithApiKey="$LANGSMITH_API_KEY" \
        --wait \
        --timeout=10m
fi

print_success "Deployment completed successfully!"

# Wait for pods to be ready
print_status "Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=$RELEASE_NAME -n $NAMESPACE --timeout=300s

# Get deployment status
print_status "Deployment status:"
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/instance=$RELEASE_NAME

# Get service information
print_status "Services:"
kubectl get svc -n $NAMESPACE -l app.kubernetes.io/instance=$RELEASE_NAME

# Get ingress information
if kubectl get ingress -n $NAMESPACE &> /dev/null; then
    print_status "Ingress:"
    kubectl get ingress -n $NAMESPACE
fi

echo ""
print_success "StudyMate deployment completed!"
echo ""
echo "📋 Access Information:"
if [ "$ENVIRONMENT" = "local" ]; then
    echo "  • Add to /etc/hosts: 127.0.0.1 $DOMAIN"
    echo "  • Application URL: http://$DOMAIN"
else
    echo "  • Application URL: https://$DOMAIN"
fi
echo ""
echo "🔧 Useful Commands:"
echo "  • View pods: kubectl get pods -n $NAMESPACE"
echo "  • View logs: kubectl logs -f deployment/studymate-client -n $NAMESPACE"
echo "  • Port forward: kubectl port-forward svc/studymate-client 8080:80 -n $NAMESPACE"
echo "  • Delete deployment: helm uninstall $RELEASE_NAME -n $NAMESPACE"
echo ""
echo "📖 For more information, see infra/helm/README.md"

# Clean up values file
rm -f "$VALUES_FILE" 