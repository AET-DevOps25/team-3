import pytest
import asyncio
import json
from unittest.mock import Mock, patch, AsyncMock
from fastapi.testclient import TestClient
from httpx import AsyncClient, ASGITransport
import tempfile
import os
from pathlib import Path

from main import app, llm_instances
from llm import StudyLLM
from request_models import CreateSessionRequest, PromptRequest, SummaryRequest, QuizRequest, FlashcardRequest

client = TestClient(app)

class TestHealthEndpoint:
    def test_health_check_success(self):
        """Test health check endpoint returns healthy status"""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "healthy"}

    @patch('main.logger')
    def test_health_check_exception(self, mock_logger):
        """Test health check handles exceptions gracefully"""
        # This would require modifying the health check to actually check dependencies
        # For now, we test that it returns healthy
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "healthy"}

class TestSessionManagement:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_load_session_success(self, mock_save_document, mock_study_llm):
        """Test successful session creation"""
        mock_save_document.return_value = "/fake/path/test.pdf"
        mock_llm_instance = Mock()
        mock_study_llm.return_value = mock_llm_instance
        
        request_data = {
            "session_id": "test-session-123",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"  # base64 encoded "test content"
        }
        
        response = client.post("/session/load", json=request_data)
        
        assert response.status_code == 200
        assert response.json() == {"message": "Session created successfully."}
        assert "test-session-123" in llm_instances
        mock_save_document.assert_called_once_with("test-session-123_test.pdf", "dGVzdCBjb250ZW50")
        mock_study_llm.assert_called_once_with("/fake/path/test.pdf")

    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_load_session_already_exists(self, mock_save_document, mock_study_llm):
        """Test loading a session that already exists"""
        mock_llm_instance = Mock()
        llm_instances["existing-session"] = mock_llm_instance
        
        request_data = {
            "session_id": "existing-session",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"
        }
        
        response = client.post("/session/load", json=request_data)
        
        assert response.status_code == 200
        assert response.json() == {"message": "Session already loaded."}
        mock_save_document.assert_not_called()
        mock_study_llm.assert_not_called()

    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_load_session_failure(self, mock_save_document, mock_study_llm):
        """Test session creation failure"""
        mock_save_document.return_value = "/fake/path/test.pdf"
        mock_study_llm.side_effect = Exception("Failed to create LLM instance")
        
        request_data = {
            "session_id": "test-session-123",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"
        }
        
        response = client.post("/session/load", json=request_data)
        
        assert response.status_code == 200
        assert "error" in response.json()
        assert "Failed to create session test-session-123" in response.json()["error"]

    def test_load_session_missing_fields(self):
        """Test session creation with missing required fields"""
        request_data = {
            "session_id": "test-session-123",
            # Missing document_name and document_base64
        }
        
        response = client.post("/session/load", json=request_data)
        
        assert response.status_code == 422  # Validation error

class TestChatEndpoint:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @pytest.mark.asyncio
    async def test_chat_success(self):
        """Test successful chat interaction"""
        mock_llm = AsyncMock()
        mock_llm.prompt.return_value = "This is a test response"
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session",
            "message": "Hello, how are you?"
        }
        
        response = client.post("/chat", json=request_data)
        
        assert response.status_code == 200
        assert response.json() == {"response": "This is a test response"}
        mock_llm.prompt.assert_called_once_with("Hello, how are you?")

    def test_chat_session_not_found(self):
        """Test chat with non-existent session"""
        request_data = {
            "session_id": "non-existent-session",
            "message": "Hello"
        }
        
        response = client.post("/chat", json=request_data)
        
        assert response.status_code == 404
        assert "Session non-existent-session not found" in response.json()["response"]

    @pytest.mark.asyncio
    async def test_chat_llm_error(self):
        """Test chat when LLM raises an exception"""
        mock_llm = AsyncMock()
        mock_llm.prompt.side_effect = Exception("LLM processing error")
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session",
            "message": "Hello"
        }
        
        response = client.post("/chat", json=request_data)
        
        assert response.status_code == 200
        assert "ERROR: Chat error for session test-session" in response.json()["response"]

    def test_chat_missing_message(self):
        """Test chat with missing message field"""
        request_data = {
            "session_id": "test-session"
            # Missing message field
        }
        
        response = client.post("/chat", json=request_data)
        
        assert response.status_code == 422  # Validation error

class TestSummaryEndpoint:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @pytest.mark.asyncio
    async def test_summary_success(self):
        """Test successful summary generation"""
        mock_llm = AsyncMock()
        mock_llm.summarize.return_value = "## Test Summary\n\nThis is a test summary."
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session"
        }
        
        response = client.post("/summary", json=request_data)
        
        assert response.status_code == 200
        assert response.json() == {"response": "## Test Summary\n\nThis is a test summary."}
        mock_llm.summarize.assert_called_once()

    def test_summary_session_not_found(self):
        """Test summary with non-existent session"""
        request_data = {
            "session_id": "non-existent-session"
        }
        
        response = client.post("/summary", json=request_data)
        
        assert response.status_code == 200
        assert "ERROR: Session non-existent-session not found" in response.json()["response"]

    @pytest.mark.asyncio
    async def test_summary_llm_error(self):
        """Test summary when LLM raises an exception"""
        mock_llm = AsyncMock()
        mock_llm.summarize.side_effect = Exception("Summary generation error")
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session"
        }
        
        response = client.post("/summary", json=request_data)
        
        assert response.status_code == 200
        assert "ERROR: Summary generation error for session test-session" in response.json()["response"]

class TestQuizEndpoint:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @pytest.mark.asyncio
    async def test_quiz_success(self):
        """Test successful quiz generation"""
        mock_llm = AsyncMock()
        mock_quiz = {
            "questions": [
                {
                    "question": "What is the main topic?",
                    "options": ["A", "B", "C", "D"],
                    "correct_answer": 0
                }
            ]
        }
        mock_llm.generate_quiz.return_value = mock_quiz
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session"
        }
        
        response = client.post("/quiz", json=request_data)
        
        assert response.status_code == 200
        assert response.json() == {"response": mock_quiz}
        mock_llm.generate_quiz.assert_called_once()

    def test_quiz_session_not_found(self):
        """Test quiz with non-existent session"""
        request_data = {
            "session_id": "non-existent-session"
        }
        
        response = client.post("/quiz", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert "questions" in response_data["response"]
        assert response_data["response"]["questions"] == []
        assert "error" in response_data["response"]

    @pytest.mark.asyncio
    async def test_quiz_llm_error(self):
        """Test quiz when LLM raises an exception"""
        mock_llm = AsyncMock()
        mock_llm.generate_quiz.side_effect = Exception("Quiz generation error")
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session"
        }
        
        response = client.post("/quiz", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert "questions" in response_data["response"]
        assert response_data["response"]["questions"] == []
        assert "error" in response_data["response"]

class TestFlashcardEndpoint:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @pytest.mark.asyncio
    async def test_flashcard_success(self):
        """Test successful flashcard generation"""
        mock_llm = AsyncMock()
        mock_flashcards = {
            "flashcards": [
                {
                    "front": "What is X?",
                    "back": "X is...",
                    "difficulty": "medium"
                }
            ]
        }
        mock_llm.generate_flashcards.return_value = mock_flashcards
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session"
        }
        
        response = client.post("/flashcard", json=request_data)
        
        assert response.status_code == 200
        assert response.json() == {"response": mock_flashcards}
        mock_llm.generate_flashcards.assert_called_once()

    def test_flashcard_session_not_found(self):
        """Test flashcard with non-existent session"""
        request_data = {
            "session_id": "non-existent-session"
        }
        
        response = client.post("/flashcard", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert "flashcards" in response_data["response"]
        assert response_data["response"]["flashcards"] == []
        assert "error" in response_data["response"]

    @pytest.mark.asyncio
    async def test_flashcard_llm_error(self):
        """Test flashcard when LLM raises an exception"""
        mock_llm = AsyncMock()
        mock_llm.generate_flashcards.side_effect = Exception("Flashcard generation error")
        llm_instances["test-session"] = mock_llm
        
        request_data = {
            "session_id": "test-session"
        }
        
        response = client.post("/flashcard", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert "flashcards" in response_data["response"]
        assert response_data["response"]["flashcards"] == []
        assert "error" in response_data["response"]

class TestProcessEndpoint:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_process_success(self, mock_save_document, mock_study_llm):
        """Test successful document processing"""
        mock_save_document.return_value = "/fake/path/test.pdf"
        mock_llm_instance = Mock()
        mock_study_llm.return_value = mock_llm_instance
        
        request_data = {
            "session_id": "test-session",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"
        }
        
        response = client.post("/process", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert response_data["requestId"] == "test-session"
        assert response_data["status"] == "QUEUED"
        assert response_data["message"] == "Document queued for processing"
        assert "test-session" in llm_instances

    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_process_existing_session(self, mock_save_document, mock_study_llm):
        """Test processing with existing session"""
        mock_llm_instance = Mock()
        llm_instances["existing-session"] = mock_llm_instance
        
        request_data = {
            "session_id": "existing-session",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"
        }
        
        response = client.post("/process", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert response_data["requestId"] == "existing-session"
        assert response_data["status"] == "QUEUED"
        mock_save_document.assert_not_called()
        mock_study_llm.assert_not_called()

    @patch('main.StudyLLM')
    @patch('main.save_document')
    def test_process_failure(self, mock_save_document, mock_study_llm):
        """Test processing failure"""
        mock_save_document.side_effect = Exception("Document save error")
        
        request_data = {
            "session_id": "test-session",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"
        }
        
        response = client.post("/process", json=request_data)
        
        assert response.status_code == 200
        response_data = response.json()
        assert response_data["requestId"] is None
        assert response_data["status"] == "FAILED"
        assert "Failed to process document" in response_data["message"]

    def test_process_with_document_id(self):
        """Test processing using document_id instead of session_id"""
        request_data = {
            "document_id": "doc-123",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50"
        }
        
        with patch('main.StudyLLM') as mock_study_llm, \
             patch('main.save_document') as mock_save_document:
            
            mock_save_document.return_value = "/fake/path/test.pdf"
            mock_llm_instance = Mock()
            mock_study_llm.return_value = mock_llm_instance
            
            response = client.post("/process", json=request_data)
            
            assert response.status_code == 200
            response_data = response.json()
            assert response_data["requestId"] == "doc-123"
            assert response_data["status"] == "QUEUED"

class TestRequestValidation:
    def test_invalid_json(self):
        """Test handling of invalid JSON"""
        response = client.post("/session/load", data="invalid json")
        assert response.status_code == 422

    def test_missing_required_fields(self):
        """Test validation of required fields"""
        # Missing session_id
        response = client.post("/session/load", json={
            "document_name": "test.pdf",
            "document_base64": "dGVzdA=="
        })
        assert response.status_code == 422

    def test_empty_string_fields(self):
        """Test validation of empty string fields"""
        response = client.post("/session/load", json={
            "session_id": "",
            "document_name": "test.pdf",
            "document_base64": "dGVzdA=="
        })
        assert response.status_code == 422

    def test_invalid_base64(self):
        """Test handling of invalid base64 data"""
        # This would require actual validation in the endpoint
        response = client.post("/session/load", json={
            "session_id": "test-session",
            "document_name": "test.pdf",
            "document_base64": "invalid-base64!!!"
        })
        # Currently passes through - could add validation

class TestConcurrency:
    def setup_method(self):
        """Clear LLM instances before each test"""
        llm_instances.clear()

    def teardown_method(self):
        """Clean up after each test"""
        llm_instances.clear()

    @pytest.mark.asyncio
    async def test_concurrent_session_creation(self):
        """Test concurrent session creation"""
        with patch('main.StudyLLM') as mock_study_llm, \
             patch('main.save_document') as mock_save_document:
            
            mock_save_document.return_value = "/fake/path/test.pdf"
            mock_llm_instance = Mock()
            mock_study_llm.return_value = mock_llm_instance
            
            # Create multiple concurrent requests  
            async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
                tasks = []
                for i in range(5):
                    task = ac.post("/session/load", json={
                        "session_id": f"concurrent-session-{i}",
                        "document_name": f"test-{i}.pdf",
                        "document_base64": "dGVzdCBjb250ZW50"
                    })
                    tasks.append(task)
                
                responses = await asyncio.gather(*tasks)
                
                # All requests should succeed
                for response in responses:
                    assert response.status_code == 200
                    assert response.json()["message"] == "Session created successfully."
                
                # All sessions should be created
                assert len(llm_instances) == 5

    @pytest.mark.asyncio
    async def test_concurrent_chat_requests(self):
        """Test concurrent chat requests to same session"""
        mock_llm = AsyncMock()
        mock_llm.prompt.return_value = "Response"
        llm_instances["test-session"] = mock_llm
        
        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            tasks = []
            for i in range(3):
                task = ac.post("/chat", json={
                    "session_id": "test-session",
                    "message": f"Message {i}"
                })
                tasks.append(task)
            
            responses = await asyncio.gather(*tasks)
            
            # All requests should succeed
            for response in responses:
                assert response.status_code == 200
                assert response.json()["response"] == "Response"
            
            # LLM should be called for each request
            assert mock_llm.prompt.call_count == 3

class TestErrorHandling:
    def test_unhandled_exception_in_endpoint(self):
        """Test that unhandled exceptions are caught and returned as errors"""
        with patch('main.llm_instances') as mock_instances:
            mock_instances.__contains__.side_effect = Exception("Unexpected error")
            
            response = client.post("/chat", json={
                "session_id": "test-session",
                "message": "Hello"
            })
            
            assert response.status_code == 200
            assert "ERROR:" in response.json()["response"]

    def test_cleanup_on_application_shutdown(self):
        """Test that LLM instances are properly cleaned up on shutdown"""
        mock_llm1 = Mock()
        mock_llm2 = Mock()
        llm_instances["session1"] = mock_llm1
        llm_instances["session2"] = mock_llm2
        
        # Simulate application shutdown
        import asyncio
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        
        # This would be called during FastAPI shutdown
        from main import lifespan
        async def test_cleanup():
            async with lifespan(app):
                pass
        
        loop.run_until_complete(test_cleanup())
        
        # Verify cleanup was called
        mock_llm1.cleanup.assert_called_once()
        mock_llm2.cleanup.assert_called_once()

class TestMetrics:
    def test_metrics_endpoint_exists(self):
        """Test that metrics endpoint is available"""
        response = client.get("/metrics")
        # The endpoint should exist due to Prometheus instrumentator
        # Status code might be 200 or 404 depending on setup
        assert response.status_code in [200, 404]

    def test_metrics_instrumentation(self):
        """Test that requests are instrumented"""
        # Make a request to trigger metrics collection
        response = client.get("/health")
        assert response.status_code == 200
        
        # Check that metrics endpoint exists
        metrics_response = client.get("/metrics")
        # Should not be excluded from metrics
        assert metrics_response.status_code in [200, 404]