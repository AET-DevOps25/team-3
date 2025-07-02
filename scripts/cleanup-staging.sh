#!/bin/bash

# Staging Cleanup Script
# This script cleans up staging environments when PRs are closed

set -e

# Configuration
PR_NUMBER=${1:-"unknown"}
DOCKER_REGISTRY="ghcr.io/aet-devops25/team-3"

echo "🧹 Starting cleanup for PR #$PR_NUMBER"

# Function to cleanup Docker images
cleanup_docker_images() {
    echo "🗑️ Cleaning up Docker images for PR #$PR_NUMBER"
    
    # Remove PR-specific images
    docker images | grep "pr-$PR_NUMBER" | awk '{print $3}' | xargs -r docker rmi -f || true
    
    # Remove dangling images
    docker image prune -f
    
    echo "✅ Docker images cleaned up"
}

# Function to cleanup GitHub Container Registry
cleanup_ghcr() {
    echo "🗑️ Cleaning up GitHub Container Registry images"
    
    # This would require GitHub API access to delete images
    # For now, we'll just log what would be cleaned up
    echo "📝 Would clean up the following images:"
    echo "   - $DOCKER_REGISTRY/client:pr-$PR_NUMBER-*"
    echo "   - $DOCKER_REGISTRY/server:pr-$PR_NUMBER-*"
    echo "   - $DOCKER_REGISTRY/genai:pr-$PR_NUMBER-*"
    
    # TODO: Implement actual cleanup using GitHub API
    # gh api repos/:owner/:repo/packages/container/:package-name/versions --jq '.[] | select(.metadata.container_metadata.tag.name | contains("pr-'$PR_NUMBER'")) | .id' | xargs -I {} gh api repos/:owner/:repo/packages/container/:package-name/versions/{} --method DELETE
}

# Function to cleanup Terraform resources
cleanup_terraform() {
    echo "🗑️ Cleaning up Terraform resources"
    
    if [ -d "infra/terraform" ]; then
        cd infra/terraform
        
        # Initialize Terraform with staging backend
        terraform init -backend-config="key=infra/staging/terraform.tfstate"
        
        # Destroy staging resources
        terraform destroy -auto-approve -var="pr_number=$PR_NUMBER"
        
        cd ../..
        echo "✅ Terraform resources cleaned up"
    else
        echo "⚠️ Terraform directory not found, skipping Terraform cleanup"
    fi
}

# Function to cleanup local files
cleanup_local_files() {
    echo "🗑️ Cleaning up local staging files"
    
    # Remove staging docker-compose file
    rm -f docker-compose.staging.yml
    
    # Remove any PR-specific configuration files
    find . -name "*pr-$PR_NUMBER*" -type f -delete 2>/dev/null || true
    
    echo "✅ Local files cleaned up"
}

# Function to display cleanup summary
display_summary() {
    echo ""
    echo "🎯 Staging Cleanup Complete!"
    echo "============================="
    echo "PR Number: #$PR_NUMBER"
    echo "Resources cleaned up:"
    echo "  ✅ Docker images"
    echo "  ✅ Terraform resources"
    echo "  ✅ Local configuration files"
    echo "  📝 GitHub Container Registry (logged for manual cleanup)"
    echo ""
    echo "💡 Note: Some resources may take a few minutes to be fully cleaned up."
    echo ""
}

# Main execution
main() {
    echo "🔍 Starting cleanup process for PR #$PR_NUMBER"
    
    # Cleanup Docker images
    cleanup_docker_images
    
    # Cleanup GitHub Container Registry (log only)
    cleanup_ghcr
    
    # Cleanup Terraform resources
    cleanup_terraform
    
    # Cleanup local files
    cleanup_local_files
    
    display_summary
}

# Run main function
main "$@" 