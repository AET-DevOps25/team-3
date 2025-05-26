package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.service.DocumentService
import de.tum.cit.aet.server.service.AIService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val aiService: AIService
) {
    // TODO: Implement document endpoints
    // - POST /upload (upload document)
    // - GET /{id} (get document)
    // - GET /course/{courseId} (list documents by course)
    // - DELETE /{id} (delete document)
    // - GET /{id}/summary (get/generate summary)
    // - GET /{id}/quiz (get/generate quiz)
    // - GET /{id}/flashcards (get/generate flashcards)
}
