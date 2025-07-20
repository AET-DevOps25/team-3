import pytest
import asyncio
from unittest.mock import Mock, patch, AsyncMock
from fastapi.testclient import TestClient
from httpx import AsyncClient, ASGITransport
import tempfile
import os

from main import app, llm_instances
from llm import StudyLLM
from request_models import (
    CreateSessionRequest,
    PromptRequest,
    SummaryRequest,
    QuizRequest,
    FlashcardRequest,
)


class TestHealthEndpoint:
    def test_health_check_success(self):
        """Test health check endpoint returns healthy status"""
        client = TestClient(app)
        response = client.get("/health")
        
        assert response.status_code == 200
        assert response.json() == {"status": "healthy"}


class TestSessionManagement:
    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_load_session_success(self, mock_save_document, mock_study_llm):
        """Test successful session creation"""
        mock_save_document.return_value = "/tmp/test_doc.pdf"
        mock_llm_instance = Mock()
        mock_study_llm.return_value = mock_llm_instance
        
        client = TestClient(app)
        
        # Clear any existing sessions
        llm_instances.clear()
        
        response = client.post("/session/load", json={
            "session_id": "test_session",
            "document_name": "test.pdf",
            "document_base64": "base64content"
        })
        
        assert response.status_code == 200
        assert response.json() == {"message": "Session created successfully."}
        assert "test_session" in llm_instances
        
        mock_save_document.assert_called_once()
        mock_study_llm.assert_called_once_with("/tmp/test_doc.pdf")

    def test_load_session_already_exists(self):
        """Test loading session when it already exists"""
        client = TestClient(app)
        
        # Pre-populate session
        llm_instances["existing_session"] = Mock()
        
        response = client.post("/session/load", json={
            "session_id": "existing_session",
            "document_name": "test.pdf",
            "document_base64": "base64content"
        })
        
        assert response.status_code == 200
        assert response.json() == {"message": "Session already loaded."}


class TestChatEndpoint:
    def test_chat_session_not_found(self):
        """Test chat endpoint when session doesn't exist"""
        client = TestClient(app)
        
        # Clear sessions
        llm_instances.clear()
        
        response = client.post("/chat", json={
            "session_id": "nonexistent",
            "message": "Hello"
        })
        
        assert response.status_code == 404
        assert "Session nonexistent not found" in response.json()["response"]

    @pytest.mark.asyncio
    async def test_chat_success(self):
        """Test successful chat interaction"""
        # Create mock LLM instance
        mock_llm = Mock()
        mock_llm.prompt = AsyncMock(return_value="Mock response")
        llm_instances["test_session"] = mock_llm
        
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post("/chat", json={
                "session_id": "test_session", 
                "message": "Test message"
            })
        
        assert response.status_code == 200
        assert response.json() == {"response": "Mock response"}
        mock_llm.prompt.assert_called_once_with("Test message")


class TestSummaryEndpoint:
    def test_summary_session_not_found(self):
        """Test summary endpoint when session doesn't exist"""
        client = TestClient(app)
        
        # Clear sessions
        llm_instances.clear()
        
        response = client.post("/summary", json={
            "session_id": "nonexistent"
        })
        
        assert response.status_code == 200
        assert "Session nonexistent not found" in response.json()["response"]

    @pytest.mark.asyncio
    async def test_summary_success(self):
        """Test successful summary generation"""
        # Create mock LLM instance
        mock_llm = Mock()
        mock_llm.summarize = AsyncMock(return_value="Mock summary")
        llm_instances["test_session"] = mock_llm
        
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post("/summary", json={
                "session_id": "test_session"
            })
        
        assert response.status_code == 200
        assert response.json() == {"response": "Mock summary"}
        mock_llm.summarize.assert_called_once()


class TestQuizEndpoint:
    @pytest.mark.asyncio
    async def test_quiz_success(self):
        """Test successful quiz generation"""
        # Create mock LLM instance
        mock_llm = Mock()
        mock_llm.generate_quiz = AsyncMock(return_value={"questions": []})
        llm_instances["test_session"] = mock_llm
        
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post("/quiz", json={
                "session_id": "test_session"
            })
        
        assert response.status_code == 200
        assert response.json() == {"response": {"questions": []}}
        mock_llm.generate_quiz.assert_called_once()


class TestFlashcardEndpoint:
    @pytest.mark.asyncio
    async def test_flashcard_success(self):
        """Test successful flashcard generation"""
        # Create mock LLM instance
        mock_llm = Mock()
        mock_llm.generate_flashcards = AsyncMock(return_value={"flashcards": []})
        llm_instances["test_session"] = mock_llm
        
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
            response = await client.post("/flashcard", json={
                "session_id": "test_session"
            })
        
        assert response.status_code == 200
        assert response.json() == {"response": {"flashcards": []}}
        mock_llm.generate_flashcards.assert_called_once()


class TestMetrics:
    def test_metrics_endpoint_exists(self):
        """Test that metrics endpoint is available"""
        client = TestClient(app)
        response = client.get("/metrics")
        
        # Should not return 404
        assert response.status_code != 404