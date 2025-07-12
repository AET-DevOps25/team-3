package de.tum.cit.aet.ai.controller

import de.tum.cit.aet.ai.dto.*
import de.tum.cit.aet.ai.service.AiService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = ["*"])
class AiController(
    private val aiService: AiService
) {
    
    private val logger = LoggerFactory.getLogger(AiController::class.java)

    @PostMapping("/process-document")
    fun processDocument(
        @RequestBody request: ProcessDocumentRequest,
        authentication: Authentication
    ): ResponseEntity<ProcessDocumentResponse> {
        logger.info("POST /api/ai/process-document - Document: {}", request.documentId)
        return try {
            val response = aiService.processDocument(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error processing document: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProcessDocumentResponse(
                    documentId = request.documentId,
                    status = "failed",
                    error = e.message
                ))
        }
    }

    @PostMapping("/generate-summary")
    fun generateSummary(
        @RequestBody request: GenerateSummaryRequest,
        authentication: Authentication
    ): ResponseEntity<GenerateSummaryResponse> {
        logger.info("POST /api/ai/generate-summary - Document: {}", request.documentId)
        return try {
            val response = aiService.generateSummary(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating summary: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenerateSummaryResponse(
                    documentId = request.documentId,
                    summary = "",
                    status = "failed",
                    error = e.message
                ))
        }
    }

    @PostMapping("/generate-quiz")
    fun generateQuiz(
        @RequestBody request: GenerateQuizRequest,
        authentication: Authentication
    ): ResponseEntity<GenerateQuizResponse> {
        logger.info("POST /api/ai/generate-quiz - Document: {}", request.documentId)
        return try {
            val response = aiService.generateQuiz(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating quiz: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenerateQuizResponse(
                    documentId = request.documentId,
                    questions = emptyList(),
                    status = "failed",
                    error = e.message
                ))
        }
    }

    @PostMapping("/generate-flashcards")
    fun generateFlashcards(
        @RequestBody request: GenerateFlashcardsRequest,
        authentication: Authentication
    ): ResponseEntity<GenerateFlashcardsResponse> {
        logger.info("POST /api/ai/generate-flashcards - Document: {}", request.documentId)
        return try {
            val response = aiService.generateFlashcards(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating flashcards: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenerateFlashcardsResponse(
                    documentId = request.documentId,
                    flashcards = emptyList(),
                    status = "failed",
                    error = e.message
                ))
        }
    }

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "healthy", "service" to "ai-service"))
    }
} 