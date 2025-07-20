#!/bin/bash

# Validate GitHub Actions Workflows
# This script checks that all workflow files are properly configured

set -e

echo "🔍 Validating GitHub Actions Workflows"
echo "======================================"

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

# Check if GitHub CLI is available
if command -v gh >/dev/null 2>&1; then
    GH_AVAILABLE=true
    print_success "GitHub CLI is available"
else
    GH_AVAILABLE=false
    print_warning "GitHub CLI not available, skipping some checks"
fi

# Check workflow files exist
WORKFLOWS_DIR=".github/workflows"
if [ ! -d "$WORKFLOWS_DIR" ]; then
    print_error "Workflows directory not found: $WORKFLOWS_DIR"
    exit 1
fi

print_status "Checking workflow files..."

WORKFLOWS=(
    "ci-tests.yml"
    "deploy-tests.yml"
    "key-functionality-tests.yml"
)

for workflow in "${WORKFLOWS[@]}"; do
    if [ -f "$WORKFLOWS_DIR/$workflow" ]; then
        print_success "Found workflow: $workflow"
    else
        print_error "Missing workflow: $workflow"
        exit 1
    fi
done

# Validate YAML syntax
print_status "Validating YAML syntax..."

if command -v yamllint >/dev/null 2>&1; then
    for workflow in "${WORKFLOWS[@]}"; do
        if yamllint "$WORKFLOWS_DIR/$workflow" >/dev/null 2>&1; then
            print_success "YAML syntax valid: $workflow"
        else
            print_error "YAML syntax invalid: $workflow"
            yamllint "$WORKFLOWS_DIR/$workflow"
            exit 1
        fi
    done
else
    print_warning "yamllint not available, skipping YAML validation"
fi

# Check for required secrets
print_status "Checking for required secrets..."

REQUIRED_SECRETS=(
    "CODECOV_TOKEN"
    "OPEN_WEBUI_API_KEY_CHAT"
    "OPEN_WEBUI_API_KEY_GEN"
)

if [ "$GH_AVAILABLE" = true ]; then
    for secret in "${REQUIRED_SECRETS[@]}"; do
        if gh secret list | grep -q "$secret"; then
            print_success "Secret configured: $secret"
        else
            print_warning "Secret not configured: $secret (may use default values)"
        fi
    done
else
    print_warning "Cannot check secrets without GitHub CLI"
fi

# Validate workflow structure
print_status "Validating workflow structure..."

validate_workflow() {
    local workflow_file="$1"
    local workflow_name="$2"
    
    print_status "Validating $workflow_name..."
    
    # Check for required fields
    if grep -q "^name:" "$workflow_file"; then
        print_success "$workflow_name: Has name field"
    else
        print_error "$workflow_name: Missing name field"
        return 1
    fi
    
    if grep -q "^on:" "$workflow_file"; then
        print_success "$workflow_name: Has trigger conditions"
    else
        print_error "$workflow_name: Missing trigger conditions"
        return 1
    fi
    
    if grep -q "^jobs:" "$workflow_file"; then
        print_success "$workflow_name: Has jobs defined"
    else
        print_error "$workflow_name: Missing jobs"
        return 1
    fi
    
    # Check for common issues
    if grep -q "timeout-minutes:" "$workflow_file"; then
        print_success "$workflow_name: Has timeout configurations"
    else
        print_warning "$workflow_name: No timeout configurations (may run indefinitely)"
    fi
    
    if grep -q "if: always()" "$workflow_file"; then
        print_success "$workflow_name: Has cleanup jobs"
    else
        print_warning "$workflow_name: No cleanup jobs for failures"
    fi
    
    return 0
}

# Validate each workflow
for workflow in "${WORKFLOWS[@]}"; do
    validate_workflow "$WORKFLOWS_DIR/$workflow" "$workflow" || exit 1
done

# Check dependencies
print_status "Checking workflow dependencies..."

# Check if test files exist
TEST_FILES=(
    "client/package.json"
    "genAi/requirements.txt"
    "genAi/requirements-test.txt"
    "microservices/auth-service/build.gradle.kts"
    "microservices/document-service/build.gradle.kts"
    "microservices/genai-service/build.gradle.kts"
)

for file in "${TEST_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found dependency file: $file"
    else
        print_warning "Missing dependency file: $file"
    fi
done

# Check for test directories
TEST_DIRS=(
    "client/src/test"
    "genAi"
    "microservices/auth-service/src/test"
    "microservices/document-service/src/test"
    "microservices/genai-service/src/test"
)

for dir in "${TEST_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        print_success "Found test directory: $dir"
    else
        print_warning "Missing test directory: $dir"
    fi
done

# Check for Docker files
DOCKER_FILES=(
    "client/Dockerfile"
    "genAi/Dockerfile"
    "microservices/auth-service/Dockerfile"
    "microservices/document-service/Dockerfile"
    "microservices/genai-service/Dockerfile"
)

for file in "${DOCKER_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found Docker file: $file"
    else
        print_warning "Missing Docker file: $file (deployment tests may fail)"
    fi
done

# Check Kubernetes configurations
K8S_FILES=(
    "infra/studymate/Chart.yaml"
    "infra/studymate/values.yaml"
    "infra/studymate/templates"
)

for file in "${K8S_FILES[@]}"; do
    if [ -e "$file" ]; then
        print_success "Found Kubernetes config: $file"
    else
        print_warning "Missing Kubernetes config: $file (deployment tests may fail)"
    fi
done

# Performance check
print_status "Analyzing workflow performance..."

# Count total jobs
TOTAL_JOBS=0
for workflow in "${WORKFLOWS[@]}"; do
    JOBS_IN_WORKFLOW=$(grep -c "^  [a-zA-Z].*:$" "$WORKFLOWS_DIR/$workflow" || echo 0)
    TOTAL_JOBS=$((TOTAL_JOBS + JOBS_IN_WORKFLOW))
done

print_status "Total jobs across all workflows: $TOTAL_JOBS"

if [ $TOTAL_JOBS -gt 30 ]; then
    print_warning "High number of jobs may impact CI performance"
elif [ $TOTAL_JOBS -gt 50 ]; then
    print_error "Excessive number of jobs will impact CI performance"
fi

# Check for parallelization
if grep -q "strategy:" "$WORKFLOWS_DIR"/*.yml; then
    print_success "Found parallelization strategies"
else
    print_warning "No parallelization strategies found"
fi

# Check for caching
if grep -q "cache:" "$WORKFLOWS_DIR"/*.yml; then
    print_success "Found dependency caching"
else
    print_warning "No dependency caching found (builds may be slow)"
fi

# Generate summary
print_status "Generating validation summary..."

echo ""
echo "📊 Validation Summary"
echo "===================="
echo ""
echo "Workflows validated: ${#WORKFLOWS[@]}"
echo "Total jobs: $TOTAL_JOBS"
echo ""

# Check if all validations passed
if [ $? -eq 0 ]; then
    print_success "All workflow validations passed!"
    echo ""
    echo "🚀 Next Steps:"
    echo "1. Commit and push the workflow files"
    echo "2. Check GitHub Actions tab for workflow runs"
    echo "3. Configure required secrets in repository settings"
    echo "4. Monitor first workflow runs for any issues"
    echo ""
    echo "📚 Documentation:"
    echo "- CI/CD Guide: README-CI.md"
    echo "- Testing Guide: README-TESTING.md"
    echo "- Quick Start: QUICK-TEST-GUIDE.md"
    echo ""
else
    print_error "Some validations failed. Please fix issues before proceeding."
    exit 1
fi

# Optional: Check if we can trigger a workflow
if [ "$GH_AVAILABLE" = true ]; then
    echo "💡 Tip: You can manually trigger workflows with:"
    echo "gh workflow run ci-tests.yml"
    echo "gh workflow run key-functionality-tests.yml"
    echo "gh workflow run deploy-tests.yml"
    echo ""
fi

echo "✅ Workflow validation completed successfully!"