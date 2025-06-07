#!/bin/bash

# Deployment script for Team 3 AET cluster
# Usage: ./deploy.sh [environment]

ENVIRONMENT=${1:-dev}
NAMESPACE="team-3"
CHART_NAME="team-3-app"
CHART_PATH="./team-3-app"

echo "🚀 Deploying Team 3 application to AET cluster..."
echo "📦 Environment: $ENVIRONMENT"
echo "🎯 Namespace: $NAMESPACE"

# Create namespace if it doesn't exist
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Build and push Docker images (if needed)
echo "🔨 Building Docker images..."
docker build -t team-3/client:latest ../../client/
docker build -t team-3/server:latest ../../server/
docker build -t team-3/genai:latest ../../genAi/

# Deploy with Helm
echo "📋 Deploying with Helm..."
helm upgrade --install $CHART_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --create-namespace \
  --values $CHART_PATH/values.yaml \
  --wait \
  --timeout 300s

echo "✅ Deployment completed!"
echo "🌐 Access your application at:"
echo "   Frontend: https://team-3.aet.tum.de"
echo "   Backend API: https://team-3-api.aet.tum.de"
echo "   GenAI Service: https://team-3-genai.aet.tum.de"

# Show deployment status
echo "📊 Deployment Status:"
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE
kubectl get ingress -n $NAMESPACE
