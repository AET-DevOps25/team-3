# Staging Environment Setup

This document describes the staging environment setup for automatic PR deployments.

## Overview

The staging environment automatically creates a dedicated EC2 instance for each pull request, allowing you to test changes in an isolated environment before merging to main.

## How It Works

### 1. Pull Request Creation
When a pull request is opened or updated:
- A new EC2 instance is created using Terraform
- Docker images are built and pushed with PR-specific tags
- The application is deployed to the staging instance
- A comment is added to the PR with the staging URL

### 2. Pull Request Closure
When a pull request is closed:
- The staging EC2 instance is destroyed
- Docker images are cleaned up
- All resources are automatically removed

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   GitHub PR     │    │  GitHub Actions │    │   AWS EC2       │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ PR Created  │ │───▶│ │ Deploy Job  │ │───▶│ │ Staging     │ │
│ │             │ │    │ │             │ │    │ │ Instance    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ │             │ │
│                 │    │                 │    │ │ ┌─────────┐ │ │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ │ │ Client  │ │ │
│ │ PR Closed   │ │───▶│ │ Cleanup Job│ │───▶│ │ │ │ 3000   │ │ │
│ │             │ │    │ │             │ │    │ │ └─────────┘ │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ │             │ │
└─────────────────┘    └─────────────────┘    │ │ ┌─────────┐ │ │
                                              │ │ │ Server  │ │ │
                                              │ │ │ │ 8080   │ │ │
                                              │ │ └─────────┘ │ │
                                              │ │             │ │
                                              │ │ ┌─────────┐ │ │
                                              │ │ │ GenAI   │ │ │
                                              │ │ │ │ 8000   │ │ │
                                              │ │ └─────────┘ │ │
                                              │ └─────────────┘ │
                                              └─────────────────┘
```

## Files and Configuration

### Terraform Configuration
- `infra/terraform/staging.tf` - Staging infrastructure definition
- `infra/terraform/main.tf` - Production infrastructure (unchanged)

### GitHub Actions
- `.github/workflows/staging-deploy.yml` - Staging deployment workflow
- `.github/workflows/deploy_vm.yml` - Production deployment (unchanged)

### Scripts
- `scripts/deploy-staging.sh` - Staging deployment script
- `scripts/cleanup-staging.sh` - Staging cleanup script

### Docker Configuration
- `docker-compose.staging.yml` - Staging docker-compose configuration

## Required Secrets

The following GitHub secrets must be configured:

```bash
# AWS Configuration
AWS_ROLE_ARN=arn:aws:iam::ACCOUNT:role/GITHUB_ACTIONS_ROLE

# SSH Access
LABUSER_SSH_BASE64=<base64_encoded_ssh_key>

# Application Secrets
DATABASE_URL=jdbc:postgresql://host:port/database
JWT_SECRET=your-jwt-secret
OPENAI_API_KEY=your-openai-api-key

# GitHub Container Registry
GITHUB_TOKEN=${{ secrets.GITHUB_TOKEN }}
```

## Usage

### Automatic Deployment
1. Create a pull request against the `main` branch
2. The staging environment will be automatically created
3. Check the PR comments for the staging URL
4. Test your changes in the staging environment
5. Close the PR to automatically clean up the staging environment

### Manual Deployment
```bash
# Deploy to staging manually
./scripts/deploy-staging.sh <PR_NUMBER> <COMMIT_SHA> <STAGING_IP>

# Clean up staging manually
./scripts/cleanup-staging.sh <PR_NUMBER>
```

### Accessing Staging Environment
Once deployed, you can access:
- **Frontend**: `http://<staging-ip>:3000`
- **Backend API**: `http://<staging-ip>:8080`
- **GenAI Service**: `http://<staging-ip>:8000`
- **Traefik Dashboard**: `http://<staging-ip>:8082`

## Cost Optimization

The staging environment is designed to minimize costs:
- Uses `t2.micro` instances (smallest available)
- Automatic cleanup when PRs are closed
- PR-specific Docker image tags for easy cleanup
- Tagged resources for automatic identification

## Monitoring and Logs

### GitHub Actions Logs
- Check the Actions tab in your GitHub repository
- Look for "Staging Environment Management" workflow

### Instance Logs
```bash
# SSH into staging instance
ssh -i key.pem ubuntu@<staging-ip>

# Check Docker containers
docker-compose -f docker-compose.staging.yml ps

# View logs
docker-compose -f docker-compose.staging.yml logs -f
```

## Troubleshooting

### Common Issues

1. **Instance Creation Fails**
   - Check AWS credentials and permissions
   - Verify the AMI ID is available in the target region
   - Ensure the key pair exists

2. **Deployment Fails**
   - Check Docker image build logs
   - Verify environment variables are set
   - Check network connectivity

3. **Services Not Responding**
   - Wait for services to fully start (up to 2 minutes)
   - Check security group rules
   - Verify port configurations

### Debug Commands
```bash
# Check instance status
aws ec2 describe-instances --instance-ids <instance-id>

# Check security group rules
aws ec2 describe-security-groups --group-ids <security-group-id>

# SSH and check services
ssh -i key.pem ubuntu@<staging-ip>
docker ps
docker logs <container-name>
```

## Security Considerations

- Staging instances are accessible from any IP (0.0.0.0/0)
- Consider restricting access to specific IP ranges for production use
- Staging environments use separate security groups
- All staging resources are tagged for easy identification and cleanup

## Future Enhancements

- [ ] Add health checks and automatic restart
- [ ] Implement staging database isolation
- [ ] Add monitoring and alerting
- [ ] Create staging-specific environment variables
- [ ] Add support for multiple concurrent PR deployments 