#!/bin/bash

# Local Development Secrets Setup
# Copy this file to set-secrets-local.sh and add your actual values
# DO NOT commit the actual secrets file to git!

echo "Setting up local development secrets..."

# Note: POSTGRES_PASSWORD and JWT_SECRET are hardcoded in values.yaml

# OpenAI API keys
export OPEN_WEBUI_API_KEY_CHAT="your_chat_api_key_here"
export OPEN_WEBUI_API_KEY_GEN="your_gen_api_key_here"

# LangSmith API key (optional)
export LANGSMITH_API_KEY="your_langsmith_api_key_here"

echo "✅ Environment variables set!"
echo "Now run: ./deploy-k8s.sh"
echo ""
echo "To verify secrets are set:"
echo "echo \$OPEN_WEBUI_API_KEY_CHAT" 
echo "echo \$OPEN_WEBUI_API_KEY_GEN"
echo "echo \$LANGSMITH_API_KEY" 