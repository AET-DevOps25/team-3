package de.tum.cit.aet.server.dto.request

data class ChatMessageRequest(
    val documentId: Long,
    val message: String
)
