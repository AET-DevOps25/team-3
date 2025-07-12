package de.tum.cit.aet.ai.dto

import com.fasterxml.jackson.databind.JsonNode

// Request DTOs
data class ProcessDocumentRequest(
    val documentId: String,
    val documentContent: String,
    val documentName: String,
    val userId: String
)

data class GenerateSummaryRequest(
    val documentId: String,
    val documentContent: String,
    val documentName: String
)

data class GenerateQuizRequest(
    val documentId: String,
    val documentContent: String,
    val documentName: String,
    val questionCount: Int = 5
)

data class GenerateFlashcardsRequest(
    val documentId: String,
    val documentContent: String,
    val documentName: String,
    val flashcardCount: Int = 10
)

// Response DTOs
data class ProcessDocumentResponse(
    val documentId: String,
    val status: String,
    val summary: String? = null,
    val processedContent: JsonNode? = null,
    val error: String? = null
)

data class GenerateSummaryResponse(
    val documentId: String,
    val summary: String,
    val status: String,
    val error: String? = null
)

data class GenerateQuizResponse(
    val documentId: String,
    val questions: List<QuestionModel>,
    val status: String,
    val error: String? = null
)

data class GenerateFlashcardsResponse(
    val documentId: String,
    val flashcards: List<FlashcardModel>,
    val status: String,
    val error: String? = null
)

// Content Models
data class QuestionModel(
    val type: String,
    val question: String,
    val correctAnswer: String,
    val points: Int,
    val options: List<String>? = null
)

data class FlashcardModel(
    val question: String,
    val answer: String,
    val difficulty: String
)

// Status DTOs
data class ProcessingStatusResponse(
    val documentId: String,
    val status: ProcessingStatus,
    val progress: Int = 0,
    val error: String? = null
)

enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

// Batch Processing DTOs
data class BatchProcessRequest(
    val documentIds: List<String>
)

data class BatchProcessResponse(
    val batchId: String,
    val status: String,
    val totalDocuments: Int,
    val processedDocuments: Int = 0,
    val failedDocuments: Int = 0,
    val errors: List<String> = emptyList()
) 