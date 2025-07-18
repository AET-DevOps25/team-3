#!/bin/bash

# Test Bitnami PostgreSQL deployment
echo "🧪 Testing Bitnami PostgreSQL deployment..."

# Add Bitnami repository
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Create test namespace
kubectl create namespace test-postgres --dry-run=client -o yaml | kubectl apply -f -

# Deploy Bitnami PostgreSQL
helm upgrade --install test-postgres bitnami/postgresql \
  --namespace test-postgres \
  --set auth.postgresPassword="testpassword123" \
  --set auth.database=testdb \
  --set primary.persistence.size=1Gi \
  --set primary.persistence.storageClass="" \
  --set metrics.enabled=false \
  --wait --timeout=5m

echo "✅ Test PostgreSQL deployed!"
echo "🔍 Checking status..."
kubectl get pods -n test-postgres
kubectl get svc -n test-postgres

echo "🧹 Cleaning up test deployment..."
helm uninstall test-postgres -n test-postgres
kubectl delete namespace test-postgres

echo "✅ Test completed successfully!" 