# StudyMate CI/CD Testing Pipeline

## 🎯 Automated Testing Overview

This repository includes comprehensive automated testing that runs on every push and pull request to ensure code quality and functionality.

## 📊 Test Status

![CI Test Suite](https://github.com/AET-DevOps25/team-3/actions/workflows/ci-tests.yml/badge.svg)
![Key Functionality Tests](https://github.com/AET-DevOps25/team-3/actions/workflows/key-functionality-tests.yml/badge.svg)
![Deployment Tests](https://github.com/AET-DevOps25/team-3/actions/workflows/deploy-tests.yml/badge.svg)

## 🔄 Testing Workflows

### 1. **CI Test Suite** (`ci-tests.yml`)
Runs on every push and pull request to `main` and `develop` branches.

**Components Tested:**
- ✅ **Client Tests** (React/TypeScript)
- ✅ **GenAI Service Tests** (Python/FastAPI)
- ✅ **Auth Service Tests** (Kotlin/Spring Boot)
- ✅ **Document Service Tests** (Kotlin/Spring Boot)
- ✅ **GenAI Service Tests** (Kotlin/Spring Boot)
- ✅ **Integration Tests** (Cross-service functionality)
- ✅ **Security Tests** (Vulnerability scanning)
- ✅ **Performance Tests** (Load testing)

**Features:**
- Parallel test execution
- Coverage reporting
- Artifact archiving
- Automatic cleanup

### 2. **Key Functionality Tests** (`key-functionality-tests.yml`)
Focused tests on core application features, runs every 6 hours.

**Core Features Tested:**
- 🔐 **Authentication & Authorization**
- 📄 **Document Management**
- 🤖 **AI Features** (Chat, Summary, Quiz, Flashcards)
- 🛡️ **Error Handling & Recovery**
- ⚡ **Performance & Scalability**

**Benefits:**
- Fast feedback on critical features
- Real-world scenario testing
- Continuous monitoring

### 3. **Deployment Tests** (`deploy-tests.yml`)
Comprehensive deployment and infrastructure testing.

**Deployment Pipeline:**
- 🐳 **Docker Build & Test**
- ☸️ **Kubernetes Deployment Test**
- 🌐 **End-to-End Tests**
- 📈 **Performance Testing**
- 🔒 **Security Testing**

## 📋 Test Matrix

| Component | Unit Tests | Integration Tests | E2E Tests | Performance Tests |
|-----------|------------|------------------|-----------|------------------|
| Client (React) | ✅ | ✅ | ✅ | ✅ |
| GenAI Service (Python) | ✅ | ✅ | ✅ | ✅ |
| Auth Service (Kotlin) | ✅ | ✅ | ✅ | ✅ |
| Document Service (Kotlin) | ✅ | ✅ | ✅ | ✅ |
| GenAI Service (Kotlin) | ✅ | ✅ | ✅ | ✅ |
| Kubernetes Deployment | ✅ | ✅ | ✅ | ✅ |

## 🚀 Running Tests Locally

### Quick Start
```bash
# Run all tests
bash run-all-tests.sh

# Run specific test suites
bash run-client-tests.sh
bash run-genai-tests.sh
bash run-microservices-tests.sh
```

### Manual Commands
```bash
# Client Tests
cd client && npm test

# GenAI Tests
cd genAi && python -m pytest -v

# Microservices Tests
cd microservices/auth-service && ./gradlew test
cd microservices/document-service && ./gradlew test
cd microservices/genai-service && ./gradlew test
```

## 📊 Test Coverage

### Current Coverage Targets
- **Minimum Coverage**: 80%
- **Critical Paths**: 95%
- **New Code**: 90%

### Coverage Reports
Coverage reports are automatically generated and uploaded as artifacts:
- **Client**: Vitest coverage report
- **GenAI**: pytest-cov HTML report
- **Microservices**: JaCoCo coverage reports

## 🔧 Test Configuration

### Environment Variables
Tests use the following environment variables:

```bash
# GenAI Service
OPEN_WEBUI_API_KEY_CHAT=test-chat-key
OPEN_WEBUI_API_KEY_GEN=test-gen-key
WEAVIATE_HOST=localhost
WEAVIATE_PORT=8083

# Client
VITE_API_BASE_URL=http://localhost:3000

# Microservices
SPRING_PROFILES_ACTIVE=test
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/test_db
```

### Test Databases
Each test suite uses isolated test databases:
- **PostgreSQL 15** for microservices
- **Weaviate 1.30.3** for vector operations
- **In-memory databases** for unit tests

## 🛠️ Test Infrastructure

### Services Used
- **GitHub Actions**: CI/CD orchestration
- **Docker**: Container testing
- **Kubernetes (kind)**: Cluster testing
- **Helm**: Deployment testing
- **Codecov**: Coverage reporting
- **Trivy**: Security scanning

### Test Parallelization
Tests run in parallel to optimize execution time:
- **Matrix strategies** for multiple service testing
- **Parallel test execution** within test suites
- **Cached dependencies** for faster builds

## 📈 Performance Benchmarks

### Response Time Targets
- **API Endpoints**: <500ms (p95)
- **Page Load**: <1000ms (p95)
- **Database Queries**: <100ms (p95)

### Load Testing
- **Concurrent Users**: 50-100
- **Request Rate**: 1000 req/min
- **Error Rate**: <1%

## 🔐 Security Testing

### Automated Security Checks
- **Dependency Vulnerability Scanning**
- **Container Security Scanning**
- **Secret Detection**
- **Kubernetes Security Validation**

### Security Policies
- No hardcoded secrets in code
- All containers run as non-root
- Resource limits enforced
- Network policies implemented

## 🚨 Failure Handling

### Test Failure Notifications
- **Pull Request Comments**: Automatic test result summaries
- **GitHub Issues**: Auto-created for persistent failures
- **Email Notifications**: For critical failures

### Retry Logic
- **Flaky Test Handling**: Automatic retry for transient failures
- **Service Startup**: Retry with backoff for service dependencies
- **Network Issues**: Retry with exponential backoff

## 📋 Test Reporting

### Artifacts Generated
- **Test Results**: JUnit XML format
- **Coverage Reports**: HTML and XML formats
- **Performance Reports**: JSON and HTML formats
- **Security Reports**: SARIF format

### Test Summaries
Each workflow generates comprehensive summaries:
- **Pass/Fail Status** for each test suite
- **Coverage Metrics** with trends
- **Performance Benchmarks** with comparisons
- **Security Scan Results** with recommendations

## 🔄 Continuous Improvement

### Test Maintenance
- **Daily Scheduled Runs**: Catch integration issues early
- **Dependency Updates**: Automated security updates
- **Test Optimization**: Regular performance improvements

### Metrics Tracking
- **Test Execution Time**: Optimize for faster feedback
- **Flaky Test Detection**: Identify and fix unreliable tests
- **Coverage Trends**: Ensure coverage doesn't regress

## 🎯 Quality Gates

### Pull Request Requirements
- ✅ All tests must pass
- ✅ Coverage threshold met
- ✅ Security scan passed
- ✅ Performance benchmarks met

### Deployment Requirements
- ✅ All CI tests passed
- ✅ Integration tests passed
- ✅ E2E tests passed
- ✅ Security validation passed

## 📚 Documentation

### Test Documentation
- **Test Strategy**: [README-TESTING.md](README-TESTING.md)
- **Quick Start**: [QUICK-TEST-GUIDE.md](QUICK-TEST-GUIDE.md)
- **Setup Script**: [setup-tests.sh](setup-tests.sh)

### Troubleshooting
- **Common Issues**: See [README-TESTING.md](README-TESTING.md#troubleshooting)
- **FAQ**: Common questions and solutions
- **Support**: Contact information for help

## 🎉 Getting Started

1. **Clone the repository**
2. **Run setup script**: `bash setup-tests.sh`
3. **Run tests**: `bash run-all-tests.sh`
4. **Check results**: Review test output and coverage reports

For detailed instructions, see [README-TESTING.md](README-TESTING.md).