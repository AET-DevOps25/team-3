#!/bin/bash

# StudyMate Microservices Development Setup
# This script sets up the development environment for microservices

set -e

echo "🔧 Setting up StudyMate Microservices Development Environment..."

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

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17 or later."
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        print_error "Java 17 or later is required. Current version: $java_version"
        exit 1
    fi
    
    print_success "Java version: $(java -version 2>&1 | head -n 1)"
    
    # Check Gradle
    if ! command -v gradle &> /dev/null; then
        print_warning "Gradle is not installed. Will use Gradle wrapper."
    else
        print_success "Gradle version: $(gradle --version | head -n 1)"
    fi
    
    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js 16 or later."
        exit 1
    fi
    
    print_success "Node.js version: $(node --version)"
    
    # Check PostgreSQL
    if ! command -v psql &> /dev/null; then
        print_warning "PostgreSQL client is not installed. You may need to install it for database operations."
    else
        print_success "PostgreSQL client available"
    fi
}

# Setup databases
setup_databases() {
    print_status "Setting up databases..."
    
    # Check if PostgreSQL is running
    if ! pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
        print_warning "PostgreSQL is not running. Please start PostgreSQL and try again."
        print_status "You can start PostgreSQL with: brew services start postgresql (macOS) or sudo systemctl start postgresql (Linux)"
        return 1
    fi
    
    # Create databases
    print_status "Creating databases..."
    
    # Create studymate_auth database
    if psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw studymate_auth; then
        print_status "Database studymate_auth already exists"
    else
        psql -h localhost -U postgres -c "CREATE DATABASE studymate_auth;" || {
            print_error "Failed to create studymate_auth database"
            return 1
        }
        print_success "Created studymate_auth database"
    fi
    
    # Create studymate_documents database
    if psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw studymate_documents; then
        print_status "Database studymate_documents already exists"
    else
        psql -h localhost -U postgres -c "CREATE DATABASE studymate_documents;" || {
            print_error "Failed to create studymate_documents database"
            return 1
        }
        print_success "Created studymate_documents database"
    fi
    
    # Create studymate_ai database
    if psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw studymate_ai; then
        print_status "Database studymate_ai already exists"
    else
        psql -h localhost -U postgres -c "CREATE DATABASE studymate_ai;" || {
            print_error "Failed to create studymate_ai database"
            return 1
        }
        print_success "Created studymate_ai database"
    fi
    
    print_success "All databases are ready"
}

# Build microservices
build_microservices() {
    print_status "Building microservices..."
    
    # Build Auth Service
    print_status "Building Auth Service..."
    cd microservices/auth-service
    if [ -f "./gradlew" ]; then
        ./gradlew build -x test
    else
        gradle build -x test
    fi
    cd ../..
    
    # Build Document Service
    print_status "Building Document Service..."
    cd microservices/document-service
    if [ -f "./gradlew" ]; then
        ./gradlew build -x test
    else
        gradle build -x test
    fi
    cd ../..
    
    # Build AI Service
    print_status "Building AI Service..."
    cd microservices/ai-service
    if [ -f "./gradlew" ]; then
        ./gradlew build -x test
    else
        gradle build -x test
    fi
    cd ../..
    
    print_success "All microservices built successfully"
}

# Install frontend dependencies
install_frontend_deps() {
    print_status "Installing frontend dependencies..."
    
    cd client
    npm install
    cd ..
    
    print_success "Frontend dependencies installed"
}

# Start services
start_services() {
    print_status "Starting services..."
    
    # Start Auth Service
    print_status "Starting Auth Service on port 8081..."
    cd microservices/auth-service
    if [ -f "./gradlew" ]; then
        ./gradlew bootRun &
    else
        gradle bootRun &
    fi
    AUTH_PID=$!
    cd ../..
    
    # Start Document Service
    print_status "Starting Document Service on port 8082..."
    cd microservices/document-service
    if [ -f "./gradlew" ]; then
        ./gradlew bootRun &
    else
        gradle bootRun &
    fi
    DOCUMENT_PID=$!
    cd ../..
    
    # Start AI Service
    print_status "Starting AI Service on port 8083..."
    cd microservices/ai-service
    if [ -f "./gradlew" ]; then
        ./gradlew bootRun &
    else
        gradle bootRun &
    fi
    AI_PID=$!
    cd ../..
    
    # Start Frontend
    print_status "Starting Frontend on port 3000..."
    cd client
    npm start &
    FRONTEND_PID=$!
    cd ..
    
    # Wait for services to start
    print_status "Waiting for services to start..."
    sleep 15
    
    # Check if services are running
    check_service() {
        local service_name=$1
        local port=$2
        local pid=$3
        
        if curl -f -s "http://localhost:$port" > /dev/null 2>&1; then
            print_success "$service_name is running on port $port"
        else
            print_warning "$service_name may not be ready yet on port $port"
        fi
    }
    
    check_service "Auth Service" 8081 $AUTH_PID
    check_service "Document Service" 8082 $DOCUMENT_PID
    check_service "AI Service" 8083 $AI_PID
    check_service "Frontend" 3000 $FRONTEND_PID
    
    # Save PIDs for cleanup
    echo $AUTH_PID > .auth.pid
    echo $DOCUMENT_PID > .document.pid
    echo $AI_PID > .ai.pid
    echo $FRONTEND_PID > .frontend.pid
    
    print_success "All services started!"
    echo ""
    echo "📋 Service URLs:"
    echo "   Frontend:        http://localhost:3000"
    echo "   Auth Service:    http://localhost:8081"
    echo "   Document Service: http://localhost:8082"
    echo "   AI Service:      http://localhost:8083"
    echo ""
    echo "🛑 To stop all services:"
    echo "   ./stop-dev-microservices.sh"
    echo ""
    echo "📊 To view logs:"
    echo "   tail -f microservices/auth-service/build/tmp/bootRun/spring.log"
    echo "   tail -f microservices/document-service/build/tmp/bootRun/spring.log"
    echo "   tail -f microservices/ai-service/build/tmp/bootRun/spring.log"
}

# Main execution
main() {
    check_prerequisites
    setup_databases
    build_microservices
    install_frontend_deps
    start_services
}

# Run main function
main "$@" 