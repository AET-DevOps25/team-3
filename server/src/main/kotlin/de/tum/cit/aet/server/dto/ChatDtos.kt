package de.tum.cit.aet.server.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ChatMessageRequest(
    val message: String,
    val documentIds: List<String> = emptyList()
)

data class ChatMessageResponse(
    val id: String,
    val content: String,
    val sender: String, // "user" or "bot"
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime,
    val sources: List<String> = emptyList(),
    val documentReferences: List<DocumentReference> = emptyList()
)

data class DocumentReference(
    val documentId: String,
    val documentName: String,
    val relevantPages: List<Int> = emptyList()
)

data class ChatSessionResponse(
    val sessionId: String,
    val messages: List<ChatMessageResponse>,
    val documentsInContext: List<String>
)

data class ChatSessionRequest(
    val documentIds: List<String>
) 