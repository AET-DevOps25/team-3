package de.tum.cit.aet.server.dto.response

import java.time.LocalDateTime

data class DocumentResponse(
    val id: Long,
    val title: String,
    val fileName: String,
    val contentType: String,
    val status: String,
    val uploadDate: LocalDateTime,
    val courseId: Long,
    val hasSummary: Boolean = false,
    val hasQuiz: Boolean = false,
    val hasFlashcards: Boolean = false
)
