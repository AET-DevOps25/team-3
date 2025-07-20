import pytest
import tempfile
import os
from unittest.mock import Mock, patch


@pytest.fixture
def temp_pdf_file():
    """Create a temporary PDF file for testing"""
    with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as temp_file:
        temp_file.write(b"Mock PDF content")
        temp_file.flush()
        yield temp_file.name
    os.unlink(temp_file.name)


@pytest.fixture
def mock_weaviate_client():
    """Mock Weaviate client"""
    mock_client = Mock()
    mock_client.close.return_value = None
    return mock_client


@pytest.fixture(autouse=True)
def mock_environment():
    """Mock environment variables for tests"""
    with patch.dict(os.environ, {
        'OPEN_WEBUI_API_KEY_CHAT': 'test-chat-key',
        'OPEN_WEBUI_API_KEY_GEN': 'test-gen-key',
        'WEAVIATE_HOST': 'localhost',
        'WEAVIATE_PORT': '8080'
    }):
        yield