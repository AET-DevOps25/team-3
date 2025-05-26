package de.tum.cit.aet.server.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class AIService(
    private val webClient: WebClient.Builder
) {
    private val aiServiceUrl = "http://localhost:8081"  // Python FastAPI service

    // TODO: Implement AI service integration methods
    // - generateSummary(documentId: Long)
    // - generateQuiz(documentId: Long)
    // - generateFlashcards(documentId: Long)
    // - chatWithDocument(documentId: Long, message: String)
}
