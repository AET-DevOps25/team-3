import pytest
import os
from unittest.mock import Mock, patch


@pytest.fixture(autouse=True)
def mock_openai_environment():
    """Mock OpenAI environment variables and LLM initialization"""
    with patch.dict(
        os.environ,
        {
            "OPEN_WEBUI_API_KEY_CHAT": "test-key-chat",
            "OPEN_WEBUI_API_KEY_GEN": "test-key-gen",
            "OPENAI_API_KEY": "test-key",
        },
    ):
        with patch("llm.ChatOpenAI") as mock_chat_openai:
            # Create mock LLM instances
            mock_llm = Mock()
            mock_chat_openai.return_value = mock_llm

            yield mock_llm


@pytest.fixture(autouse=True)
def clear_llm_instances():
    """Clear LLM instances before each test"""
    from main import llm_instances

    llm_instances.clear()
    yield
    llm_instances.clear()
