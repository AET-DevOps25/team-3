package de.tum.cit.aet.genai.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// Request DTOs for GenAI service
data class CreateSessionRequest(
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("document_name")
    val documentName: String,
    @JsonProperty("document_base64")
    val documentBase64: String
)

data class SummaryRequest(
    @JsonProperty("user_id")
    val userId: String
)

data class PromptRequest(
    @JsonProperty("user_id")
    val userId: String,
    val message: String
)

data class QuizRequest(
    @JsonProperty("user_id")
    val userId: String
)

data class FlashcardRequest(
    @JsonProperty("user_id")
    val userId: String
)

// Response DTOs from GenAI service
data class GenAiResponse(
    @JsonProperty("response")
    val response: String
)

data class GenAiMessageResponse(
    @JsonProperty("message")
    val message: String
)

data class GenAiErrorResponse(
    @JsonProperty("error")
    val error: String
)

// Flashcard models
data class FlashcardModel(
    @JsonProperty("question")
    val question: String,
    @JsonProperty("answer")
    val answer: String,
    @JsonProperty("difficulty")
    val difficulty: String
)

data class FlashcardResponse(
    @JsonProperty("response")
    val response: FlashcardsData
)

data class FlashcardsData(
    @JsonProperty("flashcards")
    val flashcards: List<FlashcardModel>
)

// Quiz models
data class QuestionModel(
    @JsonProperty("type")
    val type: String,
    @JsonProperty("question")
    val question: String,
    @JsonProperty("correct_answer")
    val correctAnswer: String,
    @JsonProperty("points")
    val points: Int,
    @JsonProperty("options")
    val options: List<String>? = null // For MCQ questions
)

data class QuizResponse(
    @JsonProperty("response")
    val response: QuizData
)

data class QuizData(
    @JsonProperty("questions")
    val questions: List<QuestionModel>
)

// Additional DTOs for quiz API
data class QuizGenerationRequest(
    val documentId: String,
    val userId: String,
    val numQuestions: Int = 10
)

// Extended QuizResponse for direct API responses
data class QuizApiResponse(
    @JsonProperty("quiz")
    val quiz: Map<String, Any>,
    @JsonProperty("documentName")
    val documentName: String? = null,
    @JsonProperty("documentId")
    val documentId: String? = null,
    @JsonProperty("status")
    val status: String = "NOT_STARTED",
    @JsonProperty("error")
    val error: String? = null
)

// Microservices specific DTOs
data class ProcessDocumentRequest(
    @field:NotBlank(message = "Document ID is required")
    @JsonProperty("documentId")
    val documentId: String,
    
    @field:NotBlank(message = "User ID is required")
    @JsonProperty("userId")
    val userId: String,
    
    @field:NotNull(message = "Processing type is required")
    @JsonProperty("processingType")
    val processingType: ProcessingType
)

enum class ProcessingType {
    SUMMARY,
    QUIZ,
    FLASHCARD,
    ALL
}

data class ProcessDocumentResponse(
    @JsonProperty("requestId")
    val requestId: String,
    @JsonProperty("status")
    val status: ProcessingStatus,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("estimatedTime")
    val estimatedTime: Int? = null // in seconds
)

enum class ProcessingStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class ProcessingStatusResponse(
    @JsonProperty("requestId")
    val requestId: String,
    @JsonProperty("status")
    val status: ProcessingStatus,
    @JsonProperty("progress")
    val progress: Int, // 0-100
    @JsonProperty("result")
    val result: JsonNode? = null,
    @JsonProperty("error")
    val error: String? = null
)

data class ChatRequest(
    @field:NotBlank(message = "Message is required")
    @JsonProperty("message")
    val message: String,
    
    @field:NotBlank(message = "User ID is required")
    @JsonProperty("userId")
    val userId: String,
    
    @JsonProperty("documentContext")
    val documentContext: String? = null
)

data class ChatResponse(
    @JsonProperty("response")
    val response: String,
    @JsonProperty("userId")
    val userId: String,
    @JsonProperty("timestamp")
    val timestamp: String
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String
) 

// Chat session management DTOs (matching frontend expectations)
data class ChatSessionRequest(
    @JsonProperty("documentIds")
    val documentIds: List<String>
)

data class ChatSessionResponse(
    @JsonProperty("userId")
    val userId: String,
    @JsonProperty("messages")
    val messages: List<ChatMessageDto> = emptyList(),
    @JsonProperty("documentsInContext")
    val documentsInContext: List<String>
)

data class ChatMessageDto(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("sender")
    val sender: String, // 'user' or 'bot'
    @JsonProperty("timestamp")
    val timestamp: String,
    @JsonProperty("sources")
    val sources: List<String>? = null,
    @JsonProperty("documentReferences")
    val documentReferences: List<DocumentReference>? = null
)

data class DocumentReference(
    @JsonProperty("documentId")
    val documentId: String,
    @JsonProperty("documentName")
    val documentName: String,
    @JsonProperty("relevantPages")
    val relevantPages: List<Int>? = null
)

data class SendMessageRequest(
    @JsonProperty("message")
    val message: String,
    @JsonProperty("documentIds")
    val documentIds: List<String>? = null
)

data class SendMessageResponse(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("content")
    val content: String,
    @JsonProperty("sender")
    val sender: String,
    @JsonProperty("timestamp")
    val timestamp: String,
    @JsonProperty("sources")
    val sources: List<String>? = null,
    @JsonProperty("documentReferences")
    val documentReferences: List<DocumentReference>? = null
) 