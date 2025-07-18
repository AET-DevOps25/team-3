#!/bin/bash

# Local StudyMate Deployment Script
# This script deploys to Rancher without requiring GitHub secrets

set -e

echo "🚀 Deploying StudyMate to Rancher (Local Mode)..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Check if .env file exists and load it
if [ -f ".env" ]; then
    print_status "Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | xargs)
else
    print_warning "No .env file found. Please set environment variables manually:"
    echo "  export OPEN_WEBUI_API_KEY_CHAT='your-chat-api-key'"
    echo "  export OPEN_WEBUI_API_KEY_GEN='your-gen-api-key'"
    echo "  export LANGSMITH_API_KEY='your-langsmith-key' (optional)"
    echo ""
fi

# Validate required secrets
print_status "Validating required secrets..."

if [ -z "$OPEN_WEBUI_API_KEY_CHAT" ]; then
    print_error "OPEN_WEBUI_API_KEY_CHAT is not set"
    echo "Set it with: export OPEN_WEBUI_API_KEY_CHAT='your-chat-api-key'"
    exit 1
fi

if [ -z "$OPEN_WEBUI_API_KEY_GEN" ]; then
    print_error "OPEN_WEBUI_API_KEY_GEN is not set"
    echo "Set it with: export OPEN_WEBUI_API_KEY_GEN='your-gen-api-key'"
    exit 1
fi

print_success "All required secrets are present"

# Check prerequisites
print_status "Checking prerequisites..."

if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed or not in PATH"
    exit 1
fi

if ! command -v helm &> /dev/null; then
    print_error "helm is not installed or not in PATH"
    exit 1
fi

# Setup kubeconfig for Rancher cluster
print_status "Setting up kubeconfig for Rancher cluster..."
mkdir -p ~/.kube
cat > ~/.kube/config << 'EOF'
apiVersion: v1
kind: Config
clusters:
- name: "student"
  cluster:
    server: "https://rancher.ase.cit.tum.de/k8s/clusters/c-m-nhcfjg9h"

users:
- name: "student"
  user:
    token: "kubeconfig-u-g7fbq4tzcsm6z76:btctl45d2sfw7fvdzplvdfzr6cnwbnnmzwtxpzjq4xjbnlp5bmrvzs"

contexts:
- name: "student"
  context:
    user: "student"
    cluster: "student"

current-context: "student"
EOF
chmod 600 ~/.kube/config

if ! kubectl cluster-info &> /dev/null; then
    print_error "Not connected to a Kubernetes cluster"
    echo "Please check your kubeconfig configuration"
    exit 1
fi

print_success "Prerequisites check passed"

# Configuration
NAMESPACE="studymate"
RELEASE_NAME="studymate"
CHART_PATH="./infra/helm"
DOMAIN="${DOMAIN:-studymate.student.k8s.aet.cit.tum.de}"

print_status "Deploying to Rancher..."
print_status "Namespace: $NAMESPACE"
print_status "Release: $RELEASE_NAME"
print_status "Domain: $DOMAIN"

# Deploy using Helm
helm upgrade --install "$RELEASE_NAME" "$CHART_PATH" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    --set-string secrets.genai.data.openWebUiApiKeyChat="$OPEN_WEBUI_API_KEY_CHAT" \
    --set-string secrets.genai.data.openWebUiApiKeyGen="$OPEN_WEBUI_API_KEY_GEN" \
    --set-string secrets.genai.data.langsmithApiKey="$LANGSMITH_API_KEY" \
    --wait \
    --timeout=10m

print_success "Deployment completed successfully!"

# Wait for pods to be ready
print_status "Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=$RELEASE_NAME -n $NAMESPACE --timeout=300s || true

# Get deployment status
print_status "Deployment status:"
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/instance=$RELEASE_NAME || echo "No pods found yet"

echo ""
print_success "StudyMate deployment completed!"
echo ""
echo "📋 Access Information:"
echo "  • Application URL: https://$DOMAIN"
echo "  • Rancher Dashboard: https://rancher.tum.de"
echo ""
echo "🔧 Useful Commands:"
echo "  • View pods: kubectl get pods -n $NAMESPACE"
echo "  • View logs: kubectl logs -f deployment/studymate-client -n $NAMESPACE"
echo "  • Delete deployment: helm uninstall $RELEASE_NAME -n $NAMESPACE" 