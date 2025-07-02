package de.tum.cit.aet.server.dto

import com.fasterxml.jackson.annotation.JsonProperty

// Request DTOs for GenAI service
data class CreateSessionRequest(
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("document_name")
    val documentName: String,
    @JsonProperty("document_base64")
    val documentBase64: String
)

data class SummaryRequest(
    @JsonProperty("session_id")
    val sessionId: String
)

data class PromptRequest(
    @JsonProperty("session_id")
    val sessionId: String,
    val message: String
)

data class QuizRequest(
    @JsonProperty("session_id")
    val sessionId: String
)

data class FlashcardRequest(
    @JsonProperty("session_id")
    val sessionId: String
)

// Response DTOs from GenAI service
data class GenAiResponse(
    val response: String
)

data class GenAiMessageResponse(
    val message: String
)

data class GenAiErrorResponse(
    val error: String
)

// Flashcard models
data class FlashcardModel(
    val question: String,
    val answer: String,
    val difficulty: String
)

data class FlashcardResponse(
    val response: FlashcardsData
)

data class FlashcardsData(
    val flashcards: List<FlashcardModel>
)

// Quiz models
data class QuestionModel(
    val type: String,
    val question: String,
    @JsonProperty("correct_answer")
    val correctAnswer: String,
    val points: Int,
    val options: List<String>? = null // For MCQ questions
)

data class QuizResponse(
    val response: QuizData
)

data class QuizData(
    val questions: List<QuestionModel>
) 