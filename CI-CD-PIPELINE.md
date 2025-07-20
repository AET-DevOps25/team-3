# CI/CD Pipeline Documentation

## Overview

This document describes the integrated CI/CD pipeline that ensures all deployments happen only after comprehensive testing has passed.

## Pipeline Flow

### 1. Trigger Events
- **Push to `main` branch**: Triggers CI Test Suite
- **Pull Request to `main`**: Triggers specific service deployments (for testing)
- **Manual Trigger**: `workflow_dispatch` allows manual deployment of any service

### 2. CI Test Suite Execution

When code is pushed to `main`, the **CI Test Suite** (`ci-tests.yml`) runs the following tests:

| Test Job | Purpose | Dependencies |
|----------|---------|--------------|
| **Client Tests** | React/TypeScript tests with coverage | Node.js 18 |
| **Auth Service Tests** | Kotlin/Spring Boot tests | Java 21, PostgreSQL |
| **Document Service Tests** | Kotlin/Spring Boot tests | Java 21, PostgreSQL |
| **Integration Tests** | End-to-end service integration | All services, Weaviate, PostgreSQL |
| **AI Functionality Tests** | GenAI service functionality | Python 3.9, Weaviate |
| **Error Handling Tests** | Client-side error scenarios | Node.js 18 |
| **Security Tests** | Vulnerability scanning & secret detection | Trivy, TruffleHog |
| **Performance Tests** | Load testing (main branch only) | k6 |

### 3. Deployment Triggers

**After CI Test Suite completes successfully**, the following services deploy based on file changes:

## Service Deployment Matrix

| Service | Deploys When Files Change In | Dependencies |
|---------|----------------------------|--------------|
| **Auth Service** | `server/auth-service/`<br>`infra/helm-charts/auth-service/`<br>`.github/workflows/deploy-auth-service.yml` | ✅ All CI tests pass |
| **Client** | `client/`<br>`infra/helm-charts/client/`<br>`.github/workflows/deploy-client.yml` | ✅ All CI tests pass |
| **Document Service** | `server/document-service/`<br>`infra/helm-charts/document-service/`<br>`.github/workflows/deploy-document-service.yml` | ✅ All CI tests pass |
| **GenAI Service** | `server/genai-service/`<br>`infra/helm-charts/genai-service/`<br>`.github/workflows/deploy-genai-service.yml` | ✅ All CI tests pass |
| **GenAI Backend** | `genAi/`<br>`infra/helm-charts/genai-backend/`<br>`.github/workflows/deploy-genai-backend.yml` | ✅ All CI tests pass |
| **Infrastructure** | `infra/helm-charts/postgres/`<br>`infra/helm-charts/weaviate/`<br>`.github/workflows/deploy-infrastructure.yml` | ✅ All CI tests pass |
| **Ingress** | `infra/helm-charts/ingress/`<br>`.github/workflows/deploy-ingress.yml` | ✅ All CI tests pass |
| **AWS Deployment** | Any change to `main` | ✅ All CI tests pass |

## Pipeline Execution Examples

### Example 1: Auth Service Update
```
1. Developer pushes changes to server/auth-service/
2. CI Test Suite runs (all 8 test jobs)
3. If ALL tests pass → Auth Service deployment triggers
4. Only Auth Service deploys (other services skip deployment)
```

### Example 2: Client + GenAI Changes
```
1. Developer pushes changes to client/ and genAi/
2. CI Test Suite runs (all 8 test jobs)
3. If ALL tests pass → Both Client and GenAI Backend deployments trigger
4. Both services deploy in parallel
```

### Example 3: Test Failure Scenario
```
1. Developer pushes changes to any service
2. CI Test Suite runs
3. If ANY test fails → NO deployments trigger
4. Developer must fix issues and push again
```

## Deployment Dependencies

### Required Test Jobs for Deployment
All deployment workflows require these test jobs to complete successfully:
- ✅ `client-tests`
- ✅ `auth-service-tests`
- ✅ `document-service-tests`
- ✅ `integration-tests`
- ✅ `ai-functionality-tests`
- ✅ `error-handling-tests`
- ✅ `security-tests`

### Optional Test Jobs
- `performance-tests` (only runs on main branch pushes)

## Manual Deployment Override

Each service supports manual deployment via GitHub Actions UI:
1. Go to Actions → Select deployment workflow
2. Click "Run workflow"
3. Choose branch and provide parameters
4. Manual deployments bypass CI test requirements

## Monitoring and Troubleshooting

### Check CI Test Status
```bash
# View latest CI run status
gh run list --workflow="CI Test Suite" --limit=5

# View specific test job logs
gh run view [RUN_ID] --job=[JOB_NAME]
```

### Check Deployment Status
```bash
# View latest deployment runs
gh run list --workflow="Deploy Auth Service" --limit=5

# View deployment logs
gh run view [RUN_ID]
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Deployment doesn't trigger | CI tests failed | Check CI test logs, fix issues |
| Service skipped deployment | No relevant file changes | Expected behavior if service unchanged |
| Multiple services deploying | Changes to multiple service directories | Expected behavior |
| Manual deployment needed | CI blocked or special deployment | Use workflow_dispatch trigger |

## Security Considerations

- **Secret Management**: All secrets managed through GitHub Secrets
- **Vulnerability Scanning**: Trivy scans all code before deployment
- **Secret Detection**: TruffleHog prevents secret commits
- **Access Control**: Kubernetes RBAC controls deployment permissions

## Performance Optimization

- **Parallel Execution**: Independent test jobs run in parallel
- **Smart Deployment**: Only changed services deploy
- **Caching**: Node.js, Gradle, and pip dependencies cached
- **Timeouts**: Reasonable timeouts prevent hanging jobs

## Best Practices

1. **Always run tests locally** before pushing to main
2. **Keep commits focused** on single services when possible
3. **Monitor CI dashboard** for test trends
4. **Use manual deployment** only for hotfixes or rollbacks
5. **Review security scan results** in GitHub Security tab