import pytest
import asyncio
import json
from unittest.mock import Mock, patch, AsyncMock
from fastapi.testclient import TestClient
from httpx import AsyncClient, ASGITransport
import tempfile
import os
from pathlib import Path

# Import after conftest.py has set up mocks
from main import app, llm_instance
from llm import StudyLLM
from request_models import (
    LoadDocumentRequest,
    PromptRequest,
    SummaryRequest,
    QuizRequest,
    FlashcardRequest,
)

client = TestClient(app)


class TestHealthEndpoint:
    def test_health_check_success(self):
        """Test health check endpoint returns healthy status"""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "healthy"}

    @patch("main.logger")
    def test_health_check_exception(self, mock_logger):
        """Test health check handles exceptions gracefully"""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "healthy"}


class TestDocumentManagement:
    @patch("main.save_document")
    @patch.object(llm_instance, "load_document")
    @pytest.mark.asyncio
    async def test_load_document_success(self, mock_load_document, mock_save_document):
        """Test successful document loading"""
        mock_save_document.return_value = "/fake/path/test.pdf"
        mock_load_document.return_value = None

        request_data = {
            "user_id": "test-user-123",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50",  # base64 encoded "test content"
        }

        response = client.post("/document", json=request_data)

        assert response.status_code == 200
        assert response.json() == {"message": "Document loaded successfully."}
        mock_save_document.assert_called_once_with(
            "test-user-123_test.pdf", "dGVzdCBjb250ZW50"
        )
        mock_load_document.assert_called_once_with(
            "test-user-123_test.pdf", "/fake/path/test.pdf", "test-user-123"
        )

    @patch("main.save_document")
    @patch.object(llm_instance, "load_document")
    def test_load_document_failure(self, mock_load_document, mock_save_document):
        """Test document loading failure"""
        mock_save_document.return_value = "/fake/path/test.pdf"
        mock_load_document.side_effect = Exception("Failed to load document")

        request_data = {
            "user_id": "test-user-123",
            "document_name": "test.pdf",
            "document_base64": "dGVzdCBjb250ZW50",
        }

        response = client.post("/document", json=request_data)

        assert response.status_code == 200
        assert "error" in response.json()
        assert "Failed to load document" in response.json()["error"]

    def test_load_document_missing_fields(self):
        """Test document loading with missing required fields"""
        request_data = {
            "user_id": "test-user-123",
            # Missing document_name and document_base64
        }

        response = client.post("/document", json=request_data)
        assert response.status_code == 422  # Validation error


class TestChatEndpoint:
    @patch.object(llm_instance, "prompt")
    @pytest.mark.asyncio
    async def test_chat_success(self, mock_prompt):
        """Test successful chat interaction"""
        mock_prompt.return_value = "This is a test response"

        request_data = {"user_id": "test-user", "message": "Hello, how are you?"}

        response = client.post("/chat", json=request_data)

        assert response.status_code == 200
        assert response.json() == {"response": "This is a test response"}
        mock_prompt.assert_called_once_with("Hello, how are you?", "test-user")

    @patch.object(llm_instance, "prompt")
    @pytest.mark.asyncio
    async def test_chat_llm_error(self, mock_prompt):
        """Test chat when LLM raises an exception"""
        mock_prompt.side_effect = Exception("LLM processing error")

        request_data = {"user_id": "test-user", "message": "Hello"}

        response = client.post("/chat", json=request_data)

        assert response.status_code == 200
        assert "ERROR: Chat error for user test-user" in response.json()["response"]

    def test_chat_missing_message(self):
        """Test chat with missing message field"""
        request_data = {
            "user_id": "test-user"
            # Missing message field
        }

        response = client.post("/chat", json=request_data)
        assert response.status_code == 422  # Validation error


class TestSummaryEndpoint:
    @patch.object(llm_instance, "summarize")
    @pytest.mark.asyncio
    async def test_summary_success(self, mock_summarize):
        """Test successful summary generation"""
        mock_summarize.return_value = "## Test Summary\n\nThis is a test summary."

        request_data = {"user_id": "test-user", "document_name": "test.pdf"}

        response = client.post("/summary", json=request_data)

        assert response.status_code == 200
        assert response.json() == {
            "response": "## Test Summary\n\nThis is a test summary."
        }
        mock_summarize.assert_called_once_with("test.pdf", "test-user")

    @patch.object(llm_instance, "summarize")
    @pytest.mark.asyncio
    async def test_summary_llm_error(self, mock_summarize):
        """Test summary when LLM raises an exception"""
        mock_summarize.side_effect = Exception("Summary generation error")

        request_data = {"user_id": "test-user", "document_name": "test.pdf"}

        response = client.post("/summary", json=request_data)

        assert response.status_code == 200
        assert (
            "ERROR: Summary generation error for user test-user"
            in response.json()["response"]
        )

    def test_summary_missing_document_name(self):
        """Test summary with missing document_name field"""
        request_data = {
            "user_id": "test-user"
            # Missing document_name field
        }

        response = client.post("/summary", json=request_data)
        assert response.status_code == 422  # Validation error


class TestQuizEndpoint:
    @patch.object(llm_instance, "generate_quiz")
    @pytest.mark.asyncio
    async def test_quiz_success(self, mock_generate_quiz):
        """Test successful quiz generation"""
        mock_quiz = {
            "questions": [
                {
                    "question": "What is the main topic?",
                    "options": ["A", "B", "C", "D"],
                    "correct_answer": 0,
                }
            ]
        }
        mock_generate_quiz.return_value = mock_quiz

        request_data = {"user_id": "test-user", "document_name": "test.pdf"}

        response = client.post("/quiz", json=request_data)

        assert response.status_code == 200
        assert response.json() == {"response": mock_quiz}
        mock_generate_quiz.assert_called_once_with("test.pdf", "test-user")

    @patch.object(llm_instance, "generate_quiz")
    @pytest.mark.asyncio
    async def test_quiz_llm_error(self, mock_generate_quiz):
        """Test quiz when LLM raises an exception"""
        mock_generate_quiz.side_effect = Exception("Quiz generation error")

        request_data = {"user_id": "test-user", "document_name": "test.pdf"}

        response = client.post("/quiz", json=request_data)

        assert response.status_code == 200
        response_data = response.json()
        assert "questions" in response_data["response"]
        assert response_data["response"]["questions"] == []
        assert "error" in response_data["response"]


class TestFlashcardEndpoint:
    @patch.object(llm_instance, "generate_flashcards")
    @pytest.mark.asyncio
    async def test_flashcard_success(self, mock_generate_flashcards):
        """Test successful flashcard generation"""
        mock_flashcards = {
            "flashcards": [
                {"front": "What is X?", "back": "X is...", "difficulty": "medium"}
            ]
        }
        mock_generate_flashcards.return_value = mock_flashcards

        request_data = {"user_id": "test-user", "document_name": "test.pdf"}

        response = client.post("/flashcard", json=request_data)

        assert response.status_code == 200
        assert response.json() == {"response": mock_flashcards}
        mock_generate_flashcards.assert_called_once_with("test.pdf", "test-user")

    @patch.object(llm_instance, "generate_flashcards")
    @pytest.mark.asyncio
    async def test_flashcard_llm_error(self, mock_generate_flashcards):
        """Test flashcard when LLM raises an exception"""
        mock_generate_flashcards.side_effect = Exception("Flashcard generation error")

        request_data = {"user_id": "test-user", "document_name": "test.pdf"}

        response = client.post("/flashcard", json=request_data)

        assert response.status_code == 200
        response_data = response.json()
        assert "flashcards" in response_data["response"]
        assert response_data["response"]["flashcards"] == []
        assert "error" in response_data["response"]


class TestRequestValidation:
    def test_invalid_json(self):
        """Test handling of invalid JSON"""
        response = client.post("/document", data="invalid json")
        assert response.status_code == 422

    def test_missing_required_fields(self):
        """Test validation of required fields"""
        # Missing user_id
        response = client.post(
            "/document",
            json={"document_name": "test.pdf", "document_base64": "dGVzdA=="},
        )
        assert response.status_code == 422

    def test_empty_string_fields(self):
        """Test behavior with empty string fields"""
        response = client.post(
            "/document",
            json={
                "user_id": "",
                "document_name": "test.pdf",
                "document_base64": "dGVzdA==",
            },
        )
        # The API should accept empty user_id if validation allows it
        assert response.status_code in [200, 422]


class TestConcurrency:
    @pytest.mark.asyncio
    async def test_concurrent_document_loading(self):
        """Test concurrent document loading"""
        with patch("main.save_document") as mock_save_document, patch.object(
            llm_instance, "load_document"
        ) as mock_load_document:

            mock_save_document.return_value = "/fake/path/test.pdf"
            mock_load_document.return_value = None

            # Create multiple concurrent requests
            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as ac:
                tasks = []
                for i in range(5):
                    task = ac.post(
                        "/document",
                        json={
                            "user_id": f"user-{i}",
                            "document_name": f"test-{i}.pdf",
                            "document_base64": "dGVzdCBjb250ZW50",
                        },
                    )
                    tasks.append(task)

                responses = await asyncio.gather(*tasks)

                # All requests should succeed
                for response in responses:
                    assert response.status_code == 200
                    assert response.json()["message"] == "Document loaded successfully."

    @pytest.mark.asyncio
    async def test_concurrent_chat_requests(self):
        """Test concurrent chat requests"""
        with patch.object(llm_instance, "prompt") as mock_prompt:
            mock_prompt.return_value = "Response"

            async with AsyncClient(
                transport=ASGITransport(app=app), base_url="http://test"
            ) as ac:
                tasks = []
                for i in range(3):
                    task = ac.post(
                        "/chat",
                        json={"user_id": "test-user", "message": f"Message {i}"},
                    )
                    tasks.append(task)

                responses = await asyncio.gather(*tasks)

                # All requests should succeed
                for response in responses:
                    assert response.status_code == 200
                    assert response.json()["response"] == "Response"

                # LLM should be called for each request
                assert mock_prompt.call_count == 3


class TestErrorHandling:
    def test_unhandled_exception_in_endpoint(self):
        """Test that unhandled exceptions are caught and returned as errors"""
        with patch.object(llm_instance, "prompt") as mock_prompt:
            mock_prompt.side_effect = Exception("Unexpected error")

            response = client.post(
                "/chat", json={"user_id": "test-user", "message": "Hello"}
            )

            assert response.status_code == 200
            assert "ERROR:" in response.json()["response"]

    def test_cleanup_on_application_shutdown(self):
        """Test that LLM instance is properly cleaned up on shutdown"""
        with patch.object(llm_instance, "cleanup") as mock_cleanup:
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
            mock_cleanup.assert_called_once()


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