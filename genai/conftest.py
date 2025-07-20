import pytest
import os
import sys
from unittest.mock import Mock, patch, MagicMock

# Set up environment variables first
os.environ.update({
    'OPEN_WEBUI_API_KEY_CHAT': 'test_api_key',
    'OPEN_WEBUI_API_KEY_GEN': 'test_api_key', 
    'OPEN_WEBUI_API_KEY_QUIZ': 'test_api_key',
    'OPEN_WEBUI_API_KEY_FLASHCARD': 'test_api_key',
    'OPEN_WEBUI_API_KEY_SUMMARY': 'test_api_key',
    'OPENAI_API_KEY': 'test_openai_key',
    'WEAVIATE_API_KEY': 'test_weaviate_key',
    'WEAVIATE_URL': 'http://test-weaviate:8080'
})

# Global mocks that need to be applied before any imports
mock_weaviate_client = Mock()
mock_weaviate_client.connect.return_value = None
mock_weaviate_client.close.return_value = None
mock_weaviate_client.collections = Mock()

mock_rag_instance = Mock()
mock_rag_instance.load_document = Mock()
mock_rag_instance.retrieve = Mock(return_value="test context")
mock_rag_instance.get_generation_chunks = Mock(return_value=[])
mock_rag_instance.cleanup = Mock()

# Apply global patches
patch('weaviate.connect_to_local', return_value=mock_weaviate_client).start()
patch('rag.RAGHelper', return_value=mock_rag_instance).start()

@pytest.fixture(autouse=True)
def setup_test_environment():
    """Setup test environment for each test"""
    # Reset mock call counts for each test
    mock_rag_instance.reset_mock()
    mock_weaviate_client.reset_mock()
    yield