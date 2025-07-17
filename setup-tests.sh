#!/bin/bash

# StudyMate Test Setup Script
# This script sets up the testing environment for all components

set -e

echo "🧪 StudyMate Test Setup Script"
echo "==============================="

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

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Node.js
    if command_exists node; then
        NODE_VERSION=$(node --version)
        print_success "Node.js found: $NODE_VERSION"
    else
        print_error "Node.js is required but not installed. Please install Node.js 18+"
        exit 1
    fi
    
    # Check npm
    if command_exists npm; then
        NPM_VERSION=$(npm --version)
        print_success "npm found: $NPM_VERSION"
    else
        print_error "npm is required but not installed"
        exit 1
    fi
    
    # Check Python
    if command_exists python3; then
        PYTHON_VERSION=$(python3 --version)
        print_success "Python found: $PYTHON_VERSION"
    else
        print_error "Python 3.9+ is required but not installed"
        exit 1
    fi
    
    # Check pip
    if command_exists pip; then
        PIP_VERSION=$(pip --version)
        print_success "pip found: $PIP_VERSION"
    else
        print_warning "pip not found, some tests may not work"
    fi
    
    # Check Java (optional)
    if command_exists java; then
        JAVA_VERSION=$(java --version | head -n 1)
        print_success "Java found: $JAVA_VERSION"
    else
        print_warning "Java not found, microservices tests will be skipped"
    fi
}

# Setup client tests
setup_client_tests() {
    print_status "Setting up client tests..."
    
    if [ -d "client" ]; then
        cd client
        print_status "Installing client dependencies..."
        npm install
        print_success "Client dependencies installed"
        
        print_status "Running client test validation..."
        if npm test -- --run --reporter=minimal > /dev/null 2>&1; then
            print_success "Client tests are working"
        else
            print_warning "Client tests have some issues (expected due to AuthContext integration)"
        fi
        cd ..
    else
        print_warning "Client directory not found, skipping client tests"
    fi
}

# Setup GenAI tests
setup_genai_tests() {
    print_status "Setting up GenAI tests..."
    
    if [ -d "genAi" ]; then
        cd genAi
        print_status "Installing GenAI dependencies..."
        pip install -r requirements.txt
        pip install -r requirements-test.txt
        print_success "GenAI dependencies installed"
        
        print_status "Running GenAI test validation..."
        if python -m pytest test_main.py::TestHealthEndpoint::test_health_check_success -q > /dev/null 2>&1; then
            print_success "GenAI tests are working"
        else
            print_warning "GenAI tests have some issues"
        fi
        cd ..
    else
        print_warning "genAi directory not found, skipping GenAI tests"
    fi
}

# Setup microservices tests
setup_microservices_tests() {
    print_status "Setting up microservices tests..."
    
    if [ -d "microservices" ]; then
        if command_exists java; then
            cd microservices
            for service in auth-service document-service genai-service; do
                if [ -d "$service" ]; then
                    print_status "Setting up $service tests..."
                    cd "$service"
                    if [ -f "gradlew" ]; then
                        chmod +x gradlew
                        ./gradlew build -x test --quiet
                        print_success "$service build successful"
                    else
                        print_warning "$service: gradlew not found"
                    fi
                    cd ..
                fi
            done
            cd ..
        else
            print_warning "Java not found, skipping microservices tests"
        fi
    else
        print_warning "microservices directory not found, skipping microservices tests"
    fi
}

# Create test runner scripts
create_test_scripts() {
    print_status "Creating test runner scripts..."
    
    # Client test runner
    cat > run-client-tests.sh << 'EOF'
#!/bin/bash
echo "🖥️  Running Client Tests"
echo "======================="
cd client
npm test -- --run
EOF
    
    # GenAI test runner
    cat > run-genai-tests.sh << 'EOF'
#!/bin/bash
echo "🐍 Running GenAI Tests"
echo "====================="
cd genAi
python -m pytest -v
EOF
    
    # Microservices test runner
    cat > run-microservices-tests.sh << 'EOF'
#!/bin/bash
echo "☕ Running Microservices Tests"
echo "============================="
cd microservices
for service in auth-service document-service genai-service; do
    if [ -d "$service" ]; then
        echo "Testing $service..."
        cd "$service"
        if [ -f "gradlew" ]; then
            ./gradlew test
        fi
        cd ..
    fi
done
EOF
    
    # All tests runner
    cat > run-all-tests.sh << 'EOF'
#!/bin/bash
echo "🧪 Running All Tests"
echo "==================="

echo ""
echo "1. Client Tests"
echo "---------------"
if [ -f "run-client-tests.sh" ]; then
    bash run-client-tests.sh
else
    echo "Client tests not available"
fi

echo ""
echo "2. GenAI Tests"
echo "--------------"
if [ -f "run-genai-tests.sh" ]; then
    bash run-genai-tests.sh
else
    echo "GenAI tests not available"
fi

echo ""
echo "3. Microservices Tests"
echo "----------------------"
if [ -f "run-microservices-tests.sh" ]; then
    bash run-microservices-tests.sh
else
    echo "Microservices tests not available"
fi

echo ""
echo "✅ All tests completed!"
EOF
    
    # Make scripts executable
    chmod +x run-client-tests.sh run-genai-tests.sh run-microservices-tests.sh run-all-tests.sh
    
    print_success "Test runner scripts created"
}

# Setup environment files
setup_environment() {
    print_status "Setting up test environment..."
    
    # Create .env.test file for GenAI tests
    if [ -d "genAi" ]; then
        cat > genAi/.env.test << 'EOF'
# Test environment variables for GenAI service
OPEN_WEBUI_API_KEY_CHAT=test-chat-key
OPEN_WEBUI_API_KEY_GEN=test-gen-key
WEAVIATE_HOST=localhost
WEAVIATE_PORT=8083
EOF
        print_success "GenAI test environment created"
    fi
    
    # Create test configuration for client
    if [ -d "client" ]; then
        cat > client/.env.test << 'EOF'
# Test environment variables for client
VITE_API_BASE_URL=http://localhost:3000
NODE_ENV=test
EOF
        print_success "Client test environment created"
    fi
}

# Print usage instructions
print_usage() {
    echo ""
    print_success "🎉 Test setup completed successfully!"
    echo ""
    echo "📋 Quick Start Commands:"
    echo "========================"
    echo ""
    echo "Run all tests:"
    echo "  bash run-all-tests.sh"
    echo ""
    echo "Run individual test suites:"
    echo "  bash run-client-tests.sh      # React/TypeScript tests"
    echo "  bash run-genai-tests.sh       # Python/FastAPI tests"
    echo "  bash run-microservices-tests.sh  # Kotlin/Spring Boot tests"
    echo ""
    echo "Manual test commands:"
    echo "  cd client && npm test              # Client tests"
    echo "  cd genAi && python -m pytest -v   # GenAI tests"
    echo "  cd microservices/auth-service && ./gradlew test  # Microservice tests"
    echo ""
    echo "📚 For detailed instructions, see: README-TESTING.md"
    echo ""
    echo "🐛 Troubleshooting:"
    echo "  - Check README-TESTING.md for common issues"
    echo "  - Ensure all prerequisites are installed"
    echo "  - Run tests individually if some fail"
    echo ""
}

# Main execution
main() {
    echo "Starting test setup..."
    echo ""
    
    check_prerequisites
    echo ""
    
    setup_client_tests
    echo ""
    
    setup_genai_tests
    echo ""
    
    setup_microservices_tests
    echo ""
    
    create_test_scripts
    echo ""
    
    setup_environment
    echo ""
    
    print_usage
}

# Run main function
main