#!/bin/bash

# Staging Deployment Script
# This script deploys the application to a staging environment for PR testing

set -e

# Configuration
PR_NUMBER=${1:-"unknown"}
COMMIT_SHA=${2:-"latest"}
STAGING_IP=${3:-"localhost"}
DOCKER_REGISTRY="ghcr.io/aet-devops25/team-3"

echo "🚀 Starting staging deployment for PR #$PR_NUMBER"

# Function to check if required environment variables are set
check_env_vars() {
    local required_vars=("DATABASE_URL" "JWT_SECRET" "OPENAI_API_KEY")
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            echo "❌ Error: Required environment variable $var is not set"
            exit 1
        fi
    done
    echo "✅ All required environment variables are set"
}

# Function to create staging docker-compose file
create_staging_compose() {
    cat > docker-compose.staging.yml << EOF
version: '3.8'

services:
  client:
    image: $DOCKER_REGISTRY/client:pr-$PR_NUMBER-$COMMIT_SHA
    ports:
      - "3000:3000"
    environment:
      - REACT_APP_API_URL=http://$STAGING_IP:8080
      - REACT_APP_GENAI_URL=http://$STAGING_IP:8000
    restart: unless-stopped
    networks:
      - studymate-network

  server:
    image: $DOCKER_REGISTRY/server:pr-$PR_NUMBER-$COMMIT_SHA
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - DATABASE_URL=$DATABASE_URL
      - JWT_SECRET=$JWT_SECRET
      - GENAI_SERVICE_URL=http://genai:8000
    restart: unless-stopped
    networks:
      - studymate-network
    depends_on:
      - genai

  genai:
    image: $DOCKER_REGISTRY/genai:pr-$PR_NUMBER-$COMMIT_SHA
    ports:
      - "8000:8000"
    environment:
      - OPENAI_API_KEY=$OPENAI_API_KEY
    restart: unless-stopped
    networks:
      - studymate-network

  traefik:
    image: traefik:v2.10
    command:
      - --api.insecure=true
      - --providers.docker=true
      - --entrypoints.web.address=:80
      - --entrypoints.websecure.address=:443
      - --log.level=INFO
    ports:
      - "80:80"
      - "443:443"
      - "8082:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    restart: unless-stopped
    networks:
      - studymate-network

networks:
  studymate-network:
    driver: bridge
EOF
}

# Function to deploy to staging server
deploy_to_staging() {
    echo "📦 Deploying to staging server at $STAGING_IP"
    
    # Create staging docker-compose file
    create_staging_compose
    
    # Copy docker-compose file to staging server
    scp -i key.pem -o StrictHostKeyChecking=no docker-compose.staging.yml ubuntu@$STAGING_IP:/home/ubuntu/
    
    # Deploy on staging server
    ssh -i key.pem -o StrictHostKeyChecking=no ubuntu@$STAGING_IP << 'EOF'
        cd /home/ubuntu
        echo "🔄 Stopping existing containers..."
        docker-compose -f docker-compose.staging.yml down || true
        
        echo "🧹 Cleaning up old images..."
        docker system prune -f
        
        echo "🚀 Starting new containers..."
        docker-compose -f docker-compose.staging.yml up -d
        
        echo "⏳ Waiting for services to be ready..."
        sleep 30
        
        echo "🔍 Checking service health..."
        docker-compose -f docker-compose.staging.yml ps
EOF
}

# Function to check deployment health
check_health() {
    echo "🏥 Checking deployment health..."
    
    # Wait for services to be ready
    sleep 10
    
    # Check if services are responding
    local services=("http://$STAGING_IP:3000" "http://$STAGING_IP:8080" "http://$STAGING_IP:8000")
    
    for service in "${services[@]}"; do
        echo "Checking $service..."
        if curl -f -s "$service" > /dev/null 2>&1; then
            echo "✅ $service is healthy"
        else
            echo "❌ $service is not responding"
            return 1
        fi
    done
    
    echo "🎉 All services are healthy!"
}

# Function to display deployment info
display_info() {
    echo ""
    echo "🎯 Staging Deployment Complete!"
    echo "=================================="
    echo "PR Number: #$PR_NUMBER"
    echo "Commit SHA: $COMMIT_SHA"
    echo "Staging URL: http://$STAGING_IP:3000"
    echo "API URL: http://$STAGING_IP:8080"
    echo "GenAI URL: http://$STAGING_IP:8000"
    echo "Traefik Dashboard: http://$STAGING_IP:8082"
    echo ""
    echo "📝 To clean up this staging environment, close the PR or run:"
    echo "   ./scripts/cleanup-staging.sh $PR_NUMBER"
    echo ""
}

# Main execution
main() {
    echo "🔍 Checking environment variables..."
    check_env_vars
    
    echo "📋 Creating staging configuration..."
    create_staging_compose
    
    echo "🚀 Deploying to staging..."
    deploy_to_staging
    
    echo "🏥 Checking deployment health..."
    check_health
    
    display_info
}

# Run main function
main "$@" 