#!/bin/bash

# StudyMate Microservices Deployment Script
# This script deploys all three microservices using Docker Compose

set -e

echo "🚀 Starting StudyMate Microservices Deployment..."

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

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Function to check if a port is available
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        print_warning "Port $port is already in use. Make sure no other services are running on this port."
        return 1
    fi
    return 0
}

# Check if required ports are available
print_status "Checking port availability..."
check_port 8081 || print_warning "Auth Service port 8081 may be in use"
check_port 8082 || print_warning "Document Service port 8082 may be in use"
check_port 8083 || print_warning "AI Service port 8083 may be in use"
check_port 3000 || print_warning "Frontend port 3000 may be in use"
check_port 5432 || print_warning "PostgreSQL port 5432 may be in use"

# Stop any existing containers
print_status "Stopping existing containers..."
docker-compose down --remove-orphans || true

# Build the images
print_status "Building Docker images..."
docker-compose build --no-cache

# Start the services
print_status "Starting microservices..."
docker-compose up -d

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 10

# Check service health
print_status "Checking service health..."

# Function to check service health
check_service_health() {
    local service_name=$1
    local port=$2
    local endpoint=$3
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost:$port$endpoint" > /dev/null 2>&1; then
            print_success "$service_name is healthy"
            return 0
        fi
        
        print_status "Waiting for $service_name to be ready... (attempt $attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service_name failed to start properly"
    return 1
}

# Check each service
check_service_health "Auth Service" 8081 "/api/auth/health" || true
check_service_health "Document Service" 8082 "/api/documents/health" || true
check_service_health "AI Service" 8083 "/api/ai/health" || true
check_service_health "Frontend" 3000 "/" || true

# Show running containers
print_status "Running containers:"
docker-compose ps

# Show service URLs
echo ""
print_success "🎉 StudyMate Microservices are now running!"
echo ""
echo "📋 Service URLs:"
echo "   Frontend:        http://localhost:3000"
echo "   Auth Service:    http://localhost:8081"
echo "   Document Service: http://localhost:8082"
echo "   AI Service:      http://localhost:8083"
echo "   Traefik Dashboard: http://localhost:8080"
echo ""
echo "📊 To view logs:"
echo "   docker-compose logs -f [service-name]"
echo ""
echo "🛑 To stop services:"
echo "   docker-compose down"
echo ""
echo "🔄 To restart services:"
echo "   docker-compose restart" 