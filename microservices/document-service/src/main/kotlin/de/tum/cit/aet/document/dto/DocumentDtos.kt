package de.tum.cit.aet.document.dto

import com.fasterxml.jackson.databind.JsonNode

data class DocumentUploadResponse(
    val documentIds: List<String>,
    val status: String,
    val message: String? = null
)

data class DocumentStatusResponse(
    val documentId: String,
    val status: DocumentStatus,
    val documentName: String,
    val uploadDate: String,
    val processingProgress: Int = 0,
    val summary: String? = null
)

enum class DocumentStatus {
    UPLOADED,        // Just uploaded
    PROCESSING,      // Being processed by AI
    PROCESSED,       // Processing complete
    READY,          // Ready for study features
    ERROR           // Processing failed
}

// Extended status response to track individual content generation
data class DocumentContentStatus(
    val documentId: String,
    val documentName: String,
    val overallStatus: DocumentStatus,
    val summaryStatus: DocumentStatus = DocumentStatus.UPLOADED,
    val quizStatus: DocumentStatus = DocumentStatus.UPLOADED,
    val flashcardStatus: DocumentStatus = DocumentStatus.UPLOADED,
    val uploadDate: String,
    val error: String? = null
)

data class DocumentMetadata(
    val documentName: String,
    val documentSize: Long,
    val pageCount: Int? = null
)

data class DocumentListResponse(
    val documents: List<DocumentInfo>
)

data class DocumentInfo(
    val id: String,
    val name: String,
    val size: Long,
    val uploadDate: String,
    val type: String,
    val status: DocumentStatus,
    val summary: String? = null
)

data class DocumentContentResponse(
    val id: String,
    val originalName: String,
    val summary: String?,
    val processedContent: JsonNode?,
    val quizData: JsonNode?,
    val flashcardData: JsonNode?,
    val status: DocumentStatus,
    val summaryStatus: DocumentStatus,
    val quizStatus: DocumentStatus,
    val flashcardStatus: DocumentStatus,
    val uploadDate: String,
    val updatedAt: String
)

data class UpdateDocumentContentRequest(
    val summary: String?,
    val processedContent: JsonNode?
)

data class FlashcardApiResponse(
    val flashcards: List<FlashcardModel>,
    val documentName: String,
    val documentId: String,
    val status: String,
    val error: String? = null
)

// Flashcard and Quiz models for AI service communication
data class FlashcardModel(
    val question: String,
    val answer: String,
    val difficulty: String
)

data class QuestionModel(
    val type: String,
    val question: String,
    val correctAnswer: String,
    val points: Int,
    val options: List<String>? = null
) 