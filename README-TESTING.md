# StudyMate Testing Guide

This guide provides comprehensive instructions for running tests across the StudyMate application, including client-side tests, GenAI service tests, and microservices tests.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Test Structure Overview](#test-structure-overview)
- [Client Tests (React/TypeScript)](#client-tests-reacttypescript)
- [GenAI Service Tests (Python/FastAPI)](#genai-service-tests-pythonfastapi)
- [Microservices Tests (Kotlin/Spring Boot)](#microservices-tests-kotlinspring-boot)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🛠️ Prerequisites

### System Requirements
- **Node.js**: v18.0.0 or higher
- **npm**: v8.0.0 or higher
- **Python**: v3.9 or higher
- **Java**: v17 or higher
- **Docker**: v20.0.0 or higher (for integration tests)

### Development Tools
- **Git**: For version control
- **IDE**: VS Code, IntelliJ IDEA, or similar
- **Terminal**: Bash, Zsh, or PowerShell

## 🏗️ Test Structure Overview

```
team-3/
├── client/                     # React frontend tests
│   ├── src/test/
│   │   ├── contexts/          # Context provider tests
│   │   ├── lib/               # API service tests
│   │   └── mocks/             # MSW mock handlers
│   ├── vitest.config.ts       # Test configuration
│   └── package.json           # Test dependencies
├── genAi/                     # Python GenAI service tests
│   ├── test_main.py           # FastAPI endpoint tests
│   ├── test_llm.py            # LLM service tests
│   ├── pytest.ini            # Test configuration
│   └── requirements-test.txt  # Test dependencies
├── microservices/             # Kotlin microservices tests
│   ├── auth-service/src/test/
│   ├── document-service/src/test/
│   └── genai-service/src/test/
└── infra/                     # Infrastructure tests
    └── studymate/templates/tests/
```

## 🖥️ Client Tests (React/TypeScript)

### Setup

1. **Navigate to client directory**:
   ```bash
   cd client
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

### Running Tests

#### Basic Test Commands

```bash
# Run all tests once
npm test

# Run tests in watch mode (recommended for development)
npm run test:watch

# Run tests with coverage report
npm run test:coverage
```

#### Advanced Test Commands

```bash
# Run specific test file
npm test -- src/test/lib/api.test.ts

# Run tests matching a pattern
npm test -- --grep "authentication"

# Run tests with verbose output
npm test -- --reporter=verbose

# Run tests and generate HTML coverage report
npm run test:coverage -- --reporter=html
```

### Test Categories

#### 1. **Authentication Tests**
```bash
# Run authentication context tests
npm test -- src/test/contexts/AuthContext.test.tsx
```

**Coverage:**
- User login/logout flows
- Token management
- Session persistence
- Error handling

#### 2. **API Service Tests**
```bash
# Run API integration tests
npm test -- src/test/lib/api.test.ts
```

**Coverage:**
- HTTP request/response handling
- Authentication headers
- Error response processing
- Rate limiting

#### 3. **Component Tests**
```bash
# Run component tests (when implemented)
npm test -- src/test/components/
```

### Test Configuration

The client tests use:
- **Vitest**: Fast test runner with ES modules support
- **React Testing Library**: Component testing utilities
- **MSW**: Mock Service Worker for API mocking
- **jsdom**: Browser environment simulation

### Mock Configuration

Tests use MSW to mock API responses:
```typescript
// Mock API responses are defined in:
// src/test/mocks/handlers.ts
```

### Expected Results

```bash
✅ Test Files:  2 passed (2)
✅ Tests:      45 passed (45)
📊 Coverage:   80%+ for critical paths
⏱️ Duration:   ~10-15 seconds
```

## 🐍 GenAI Service Tests (Python/FastAPI)

### Setup

1. **Navigate to GenAI directory**:
   ```bash
   cd genAi
   ```

2. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   pip install -r requirements-test.txt
   ```

### Running Tests

#### Basic Test Commands

```bash
# Run all tests
python -m pytest

# Run with verbose output
python -m pytest -v

# Run with coverage
python -m pytest --cov=. --cov-report=html

# Run specific test file
python -m pytest test_main.py -v
```

#### Advanced Test Commands

```bash
# Run specific test class
python -m pytest test_main.py::TestHealthEndpoint -v

# Run specific test method
python -m pytest test_main.py::TestHealthEndpoint::test_health_check_success -v

# Run tests in parallel
python -m pytest -n auto

# Run tests with benchmark
python -m pytest --benchmark-only

# Run tests with timeout
python -m pytest --timeout=60
```

### Test Categories

#### 1. **FastAPI Endpoint Tests**
```bash
# Run main API endpoint tests
python -m pytest test_main.py -v
```

**Coverage:**
- Health check endpoint
- Session management endpoints
- Chat functionality
- Summary generation
- Quiz and flashcard creation
- Error handling

#### 2. **LLM Service Tests**
```bash
# Run LLM service tests
python -m pytest test_llm.py -v
```

**Coverage:**
- LLM initialization
- Prompt processing
- Document summarization
- Flashcard generation
- Resource cleanup

#### 3. **Integration Tests**
```bash
# Run integration tests
python -m pytest -m integration -v
```

### Test Configuration

The GenAI tests use:
- **pytest**: Python testing framework
- **pytest-asyncio**: Async test support
- **pytest-mock**: Mocking utilities
- **httpx**: HTTP client testing
- **FastAPI TestClient**: API testing

### Mock Configuration

Tests use comprehensive mocking:
```python
# Mock external dependencies
@patch('main.StudyLLM')
@patch('main.save_document')
```

### Expected Results

```bash
✅ Test Files:  2 passed (2)
✅ Tests:      28 passed, 5 failed (33 total)
📊 Coverage:   84% success rate
⏱️ Duration:   ~60-90 seconds
```

## ☕ Microservices Tests (Kotlin/Spring Boot)

### Setup

1. **Navigate to microservice directory**:
   ```bash
   cd microservices/auth-service
   # or
   cd microservices/document-service
   # or  
   cd microservices/genai-service
   ```

2. **Ensure Java 17 is installed**:
   ```bash
   java --version
   ```

### Running Tests

#### Basic Test Commands

```bash
# Run all tests
./gradlew test

# Run with detailed output
./gradlew test --info

# Run specific test class
./gradlew test --tests "AuthControllerTest"

# Run tests continuously
./gradlew test --continuous
```

#### Advanced Test Commands

```bash
# Run integration tests only
./gradlew integrationTest

# Run with coverage report
./gradlew test jacocoTestReport

# Run with test report
./gradlew test testReport

# Clean and test
./gradlew clean test
```

### Test Categories

#### 1. **Controller Tests**
```bash
# Test REST API endpoints
./gradlew test --tests "*ControllerTest"
```

#### 2. **Service Layer Tests**
```bash
# Test business logic
./gradlew test --tests "*ServiceTest"
```

#### 3. **Repository Tests**
```bash
# Test data access layer
./gradlew test --tests "*RepositoryTest"
```

### Test Configuration

The microservices tests use:
- **JUnit 5**: Testing framework
- **Spring Boot Test**: Spring testing utilities
- **Testcontainers**: Database testing
- **MockMvc**: Web layer testing

### Expected Results

```bash
✅ Build:      SUCCESS
✅ Tests:      All tests pass (when implemented)
📊 Coverage:   Target 80%+
⏱️ Duration:   ~30-60 seconds per service
```

## 🚀 CI/CD Integration

### GitHub Actions

The project includes automated testing in CI/CD:

```yaml
# .github/workflows/test.yml
name: Test Suite
on: [push, pull_request]
jobs:
  client-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: cd client && npm ci
      - run: cd client && npm test
  
  genai-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      - run: cd genAi && pip install -r requirements.txt requirements-test.txt
      - run: cd genAi && python -m pytest
  
  microservices-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: cd microservices/auth-service && ./gradlew test
```

### Pre-commit Hooks

Enable automatic testing before commits:

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install

# Run hooks manually
pre-commit run --all-files
```

### Test Coverage Requirements

- **Minimum Coverage**: 80%
- **Critical Paths**: 95%
- **New Code**: 90%

## 🔧 Troubleshooting

### Common Issues

#### 1. **Client Tests Failing**

**Problem**: Tests timeout or fail to run
```bash
Error: Test timeout after 60000ms
```

**Solution**:
```bash
# Clear node modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Update test timeout
npm test -- --timeout 120000
```

#### 2. **GenAI Tests Import Errors**

**Problem**: Module not found errors
```bash
ModuleNotFoundError: No module named 'langchain_openai'
```

**Solution**:
```bash
# Install all dependencies
pip install -r requirements.txt
pip install -r requirements-test.txt

# Use virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt requirements-test.txt
```

#### 3. **Microservices Tests Build Errors**

**Problem**: Gradle build fails
```bash
Task :test FAILED
```

**Solution**:
```bash
# Clean and rebuild
./gradlew clean build

# Check Java version
java --version

# Update Gradle wrapper
./gradlew wrapper --gradle-version 8.5
```

### Environment Variables

Some tests require environment variables:

```bash
# For GenAI tests
export OPEN_WEBUI_API_KEY_CHAT=test-key
export OPEN_WEBUI_API_KEY_GEN=test-key
export WEAVIATE_HOST=localhost
export WEAVIATE_PORT=8083

# For client tests
export VITE_API_BASE_URL=http://localhost:3000
```

### Database Setup

For integration tests requiring databases:

```bash
# Start test database with Docker
docker run -d --name test-postgres -p 5432:5432 -e POSTGRES_DB=testdb -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test postgres:15

# Start Weaviate for GenAI tests
docker run -d --name test-weaviate -p 8083:8080 cr.weaviate.io/semitechnologies/weaviate:1.30.3
```

## 📊 Test Reports

### Coverage Reports

View test coverage reports:

```bash
# Client coverage
cd client && npm run test:coverage
# Open: coverage/index.html

# GenAI coverage
cd genAi && python -m pytest --cov=. --cov-report=html
# Open: htmlcov/index.html

# Microservices coverage
cd microservices/auth-service && ./gradlew test jacocoTestReport
# Open: build/reports/jacoco/test/html/index.html
```

### Test Result Reports

Generate detailed test reports:

```bash
# Client test report
cd client && npm test -- --reporter=json > test-results.json

# GenAI test report
cd genAi && python -m pytest --junitxml=test-results.xml

# Microservices test report
cd microservices/auth-service && ./gradlew test testReport
```

## 🤝 Contributing

### Writing New Tests

#### Client Tests
```typescript
// src/test/components/MyComponent.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import MyComponent from '../../components/MyComponent'

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent />)
    expect(screen.getByText('Hello World')).toBeInTheDocument()
  })
})
```

#### GenAI Tests
```python
# test_new_feature.py
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

class TestNewFeature:
    def test_new_endpoint(self):
        response = client.get("/new-endpoint")
        assert response.status_code == 200
        assert response.json() == {"message": "success"}
```

#### Microservices Tests
```kotlin
// NewFeatureTest.kt
@SpringBootTest
class NewFeatureTest {
    @Test
    fun `should handle new feature request`() {
        // Test implementation
        assertThat(result).isNotNull()
    }
}
```

### Test Guidelines

1. **Follow AAA Pattern**: Arrange, Act, Assert
2. **Use Descriptive Names**: `should_return_error_when_user_not_found`
3. **Mock External Dependencies**: APIs, databases, file systems
4. **Test Edge Cases**: Empty inputs, null values, boundary conditions
5. **Keep Tests Independent**: Each test should run in isolation

### Performance Testing

For performance testing:

```bash
# Client performance tests
cd client && npm run test:performance

# GenAI benchmark tests
cd genAi && python -m pytest --benchmark-only

# Load testing with k6
k6 run load-test.js
```

## 📚 Additional Resources

- [Vitest Documentation](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [pytest Documentation](https://docs.pytest.org/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [MSW Documentation](https://mswjs.io/)

## 🆘 Support

If you encounter issues:

1. **Check this guide** for common solutions
2. **Review test logs** for specific error messages
3. **Check GitHub Issues** for known problems
4. **Contact the team** via Slack or email

---

**Happy Testing! 🧪**