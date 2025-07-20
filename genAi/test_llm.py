import pytest
from unittest.mock import Mock, patch, AsyncMock
import tempfile
import os
from pathlib import Path

from llm import StudyLLM


class TestStudyLLM:
    @pytest.fixture
    def mock_rag_helper(self):
        """Create a mock RAGHelper instance"""
        mock_rag = Mock()
        mock_rag.retrieve.return_value = "Mock retrieved context"
        mock_rag.summary_chunks = [
            Mock(page_content="Chunk 1 content"),
            Mock(page_content="Chunk 2 content"),
        ]
        mock_rag.cleanup.return_value = None
        return mock_rag

    @pytest.fixture
    def mock_chat_llm(self):
        """Create a mock ChatOpenAI instance for chat"""
        mock_llm = Mock()
        mock_response = Mock()
        mock_response.content = "Mock chat response"
        mock_llm.ainvoke = AsyncMock(return_value=mock_response)
        return mock_llm

    @pytest.fixture
    def temp_pdf_file(self):
        """Create a temporary PDF file for testing"""
        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as temp_file:
            temp_file.write(b"Mock PDF content")
            temp_file.flush()
            yield temp_file.name
        os.unlink(temp_file.name)

    @patch('llm.RAGHelper')
    def test_init_success(self, mock_rag_class, temp_pdf_file):
        """Test successful StudyLLM initialization"""
        mock_rag_instance = Mock()
        mock_rag_class.return_value = mock_rag_instance
        
        study_llm = StudyLLM(temp_pdf_file)
        
        mock_rag_class.assert_called_once_with(temp_pdf_file)
        assert study_llm.rag_helper == mock_rag_instance

    @patch('llm.RAGHelper')
    def test_init_with_invalid_path(self, mock_rag_class):
        """Test StudyLLM initialization with invalid file path"""
        mock_rag_class.side_effect = ValueError("Error loading document")
        
        with pytest.raises(ValueError, match="Error initializing RAGHelper"):
            StudyLLM("invalid_path.pdf")

    @patch('llm.RAGHelper')
    async def test_prompt_success(self, mock_rag_class, mock_chat_llm, temp_pdf_file):
        """Test successful prompt processing"""
        mock_rag_instance = Mock()
        mock_rag_instance.retrieve.return_value = "Retrieved context"
        mock_rag_class.return_value = mock_rag_instance
        
        with patch.object(StudyLLM, '_get_chat_llm', return_value=mock_chat_llm):
            study_llm = StudyLLM(temp_pdf_file)
            
            response = await study_llm.prompt("Test question")
            
            assert response == "Mock chat response"
            mock_rag_instance.retrieve.assert_called_once_with("Test question", top_k=5)
            mock_chat_llm.ainvoke.assert_called_once()

    @patch('llm.RAGHelper')
    def test_cleanup_success(self, mock_rag_class, temp_pdf_file):
        """Test successful cleanup"""
        mock_rag_instance = Mock()
        mock_rag_class.return_value = mock_rag_instance
        
        study_llm = StudyLLM(temp_pdf_file)
        study_llm.cleanup()
        
        mock_rag_instance.cleanup.assert_called_once()

    def test_lazy_initialization(self):
        """Test that LLM instances are lazily initialized"""
        # Reset class attributes
        StudyLLM._chat_llm = None
        StudyLLM._generation_llm = None
        
        with patch('llm.ChatOpenAI') as mock_chat_openai:
            mock_instance = Mock()
            mock_chat_openai.return_value = mock_instance
            
            study_llm = StudyLLM.__new__(StudyLLM)
            
            # First access should create the instance
            chat_llm = study_llm.chat_llm
            assert chat_llm == mock_instance
            mock_chat_openai.assert_called_once()
            
            # Second access should return the same instance
            mock_chat_openai.reset_mock()
            chat_llm2 = study_llm.chat_llm
            assert chat_llm2 == mock_instance
            mock_chat_openai.assert_not_called()

    def test_property_setters_and_deleters(self):
        """Test LLM property setters and deleters work for testing"""
        study_llm = StudyLLM.__new__(StudyLLM)
        
        # Test setter
        mock_llm = Mock()
        study_llm.chat_llm = mock_llm
        assert StudyLLM._chat_llm == mock_llm
        
        # Test deleter
        del study_llm.chat_llm
        assert StudyLLM._chat_llm is None