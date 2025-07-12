#!/bin/bash

# StudyMate Microservices Development Stop Script
# This script stops all running microservices

echo "🛑 Stopping StudyMate Microservices..."

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

# Function to kill process by PID file
kill_service() {
    local service_name=$1
    local pid_file=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            print_status "Stopping $service_name (PID: $pid)..."
            kill $pid
            sleep 2
            
            # Check if process is still running
            if ps -p $pid > /dev/null 2>&1; then
                print_warning "$service_name is still running, force killing..."
                kill -9 $pid
            fi
            
            print_success "$service_name stopped"
        else
            print_warning "$service_name was not running"
        fi
        rm -f "$pid_file"
    else
        print_warning "PID file for $service_name not found"
    fi
}

# Stop all services
print_status "Stopping all services..."

kill_service "Auth Service" ".auth.pid"
kill_service "Document Service" ".document.pid"
kill_service "AI Service" ".ai.pid"
kill_service "Frontend" ".frontend.pid"

# Kill any remaining Java processes for our services
print_status "Cleaning up any remaining processes..."

# Kill processes on specific ports
for port in 8081 8082 8083 3000; do
    pid=$(lsof -ti:$port 2>/dev/null)
    if [ ! -z "$pid" ]; then
        print_status "Killing process on port $port (PID: $pid)..."
        kill $pid 2>/dev/null || true
    fi
done

# Kill any remaining Gradle daemons for our project
print_status "Stopping Gradle daemons..."
cd microservices/auth-service && ./gradlew --stop 2>/dev/null || true
cd ../document-service && ./gradlew --stop 2>/dev/null || true
cd ../ai-service && ./gradlew --stop 2>/dev/null || true
cd ../..

print_success "All services stopped!"
echo ""
echo "🧹 Cleanup complete. All StudyMate microservices have been stopped." 