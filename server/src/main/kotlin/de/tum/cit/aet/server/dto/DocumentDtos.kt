package de.tum.cit.aet.server.dto

import com.fasterxml.jackson.databind.JsonNode

data class DocumentUploadResponse(
    val documentIds: List<String>,
    val status: String
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
    val status: DocumentStatus,
    val uploadDate: String,
    val updatedAt: String
)

data class UpdateDocumentContentRequest(
    val summary: String?,
    val processedContent: JsonNode?
) 