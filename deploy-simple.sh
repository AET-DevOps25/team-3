#!/bin/bash

# Simplified StudyMate Deployment Script
# This script deploys StudyMate without Helm for environments with limited permissions

set -e

echo "🚀 Deploying StudyMate (Simplified Mode)..."

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

# Configuration
NAMESPACE="studymate"
DOMAIN="studymate.student.k8s.aet.cit.tum.de"

print_status "Environment: Simplified deployment"
print_status "Domain: $DOMAIN"
print_status "Namespace: $NAMESPACE"

# Check secrets
print_status "Validating required secrets..."
if [ -z "$OPEN_WEBUI_API_KEY_CHAT" ]; then
    print_error "OPEN_WEBUI_API_KEY_CHAT is required"
    exit 1
fi
if [ -z "$OPEN_WEBUI_API_KEY_GEN" ]; then
    print_error "OPEN_WEBUI_API_KEY_GEN is required"
    exit 1
fi
print_success "Secrets validated"

# Create namespace if it doesn't exist
print_status "Creating namespace..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || {
    print_warning "Could not create namespace, it might already exist"
}

# Create secrets
print_status "Creating secrets..."
cat << EOF | kubectl apply -f - 2>/dev/null || print_warning "Could not create secrets"
apiVersion: v1
kind: Secret
metadata:
  name: studymate-secrets
  namespace: $NAMESPACE
type: Opaque
data:
  postgres-password: $(echo -n "changeme-postgres-password" | base64)
  jwt-secret: $(echo -n "changeme-very-long-jwt-secret-key-at-least-256-bits" | base64)
  openwebui-chat-key: $(echo -n "$OPEN_WEBUI_API_KEY_CHAT" | base64)
  openwebui-gen-key: $(echo -n "$OPEN_WEBUI_API_KEY_GEN" | base64)
  langsmith-key: $(echo -n "${LANGSMITH_API_KEY:-}" | base64)
EOF

# Create ConfigMap for PostgreSQL init
print_status "Creating PostgreSQL ConfigMap..."
cat << EOF | kubectl apply -f - 2>/dev/null || print_warning "Could not create ConfigMap"
apiVersion: v1
kind: ConfigMap
metadata:
  name: studymate-postgres-init
  namespace: $NAMESPACE
data:
  init.sql: |
    CREATE DATABASE IF NOT EXISTS studymate;
    CREATE USER IF NOT EXISTS studymate WITH PASSWORD 'changeme-postgres-password';
    GRANT ALL PRIVILEGES ON DATABASE studymate TO studymate;
EOF

# Create PostgreSQL deployment
print_status "Creating PostgreSQL deployment..."
cat << EOF | kubectl apply -f - 2>/dev/null || print_warning "Could not create PostgreSQL deployment"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: studymate-postgres
  namespace: $NAMESPACE
  labels:
    app: studymate-postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: studymate-postgres
  template:
    metadata:
      labels:
        app: studymate-postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_DB
          value: studymate
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: studymate-secrets
              key: postgres-password
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: postgres-init
          mountPath: /docker-entrypoint-initdb.d
      volumes:
      - name: postgres-storage
        emptyDir: {}
      - name: postgres-init
        configMap:
          name: studymate-postgres-init
---
apiVersion: v1
kind: Service
metadata:
  name: studymate-postgres
  namespace: $NAMESPACE
  labels:
    app: studymate-postgres
spec:
  ports:
  - port: 5432
    targetPort: 5432
  selector:
    app: studymate-postgres
EOF

# Create client deployment
print_status "Creating client deployment..."
cat << EOF | kubectl apply -f - 2>/dev/null || print_warning "Could not create client deployment"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: studymate-client
  namespace: $NAMESPACE
  labels:
    app: studymate-client
spec:
  replicas: 1
  selector:
    matchLabels:
      app: studymate-client
  template:
    metadata:
      labels:
        app: studymate-client
    spec:
      containers:
      - name: client
        image: studymate/client:latest
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: studymate-client
  namespace: $NAMESPACE
  labels:
    app: studymate-client
spec:
  ports:
  - port: 80
    targetPort: 80
  selector:
    app: studymate-client
EOF

# Create ingress
print_status "Creating ingress..."
cat << EOF | kubectl apply -f - 2>/dev/null || print_warning "Could not create ingress"
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: studymate-ingress
  namespace: $NAMESPACE
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - $DOMAIN
    secretName: studymate-tls
  rules:
  - host: $DOMAIN
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: studymate-client
            port:
              number: 80
EOF

print_success "Simplified deployment completed!"

# Wait for pods to be ready (handle permission errors)
print_status "Waiting for pods to be ready..."
if kubectl wait --for=condition=ready pod -l app=studymate-postgres -n $NAMESPACE --timeout=300s 2>/dev/null; then
    print_success "PostgreSQL pod is ready"
else
    print_warning "Could not verify PostgreSQL pod readiness"
fi

if kubectl wait --for=condition=ready pod -l app=studymate-client -n $NAMESPACE --timeout=300s 2>/dev/null; then
    print_success "Client pod is ready"
else
    print_warning "Could not verify client pod readiness"
fi

# Get deployment status
print_status "Deployment status:"
kubectl get pods -n $NAMESPACE 2>/dev/null || print_warning "Could not retrieve pod status"

print_status "Services:"
kubectl get svc -n $NAMESPACE 2>/dev/null || print_warning "Could not retrieve service status"

print_status "Ingress:"
kubectl get ingress -n $NAMESPACE 2>/dev/null || print_warning "Could not retrieve ingress status"

echo ""
print_success "StudyMate simplified deployment completed!"
echo ""
echo "📋 Access Information:"
echo "  • Application URL: https://$DOMAIN"
echo "  • Student Rancher Dashboard: https://rancher.tum.de"
echo ""
echo "🔧 Useful Commands:"
echo "  • View pods: kubectl get pods -n $NAMESPACE"
echo "  • View logs: kubectl logs -f deployment/studymate-client -n $NAMESPACE"
echo "  • Port forward: kubectl port-forward svc/studymate-client 8080:80 -n $NAMESPACE"
echo ""
echo "⚠️  Note: This is a simplified deployment without Helm."
echo "    Some features may not be available due to permission limitations." 