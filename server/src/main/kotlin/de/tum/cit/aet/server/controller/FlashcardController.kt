package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.FlashcardApiResponse
import de.tum.cit.aet.server.service.FlashcardService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/flashcards")
@CrossOrigin(origins = ["*"])
class FlashcardController(
    private val flashcardService: FlashcardService
) {
    
    private val logger = LoggerFactory.getLogger(FlashcardController::class.java)

    @PostMapping("/generate")
    fun generateFlashcards(@RequestBody request: Map<String, String>): ResponseEntity<FlashcardApiResponse> {
        return try {
            logger.info("POST /api/flashcards/generate")

            val response = flashcardService.generateFlashcards(request["documentId"] ?: "")
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(FlashcardApiResponse(flashcards = emptyList(), documentName = "", documentId = "", status = "FAILED", error = e.message))
        }
    }

    @GetMapping("/documents/{documentId}")
    fun getFlashcardsForDocument(@PathVariable documentId: String): ResponseEntity<FlashcardApiResponse> {
        return try {
            logger.info("GET /api/flashcards/documents/{}", documentId)

            val response = flashcardService.generateFlashcards(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(FlashcardApiResponse(flashcards = emptyList(), documentName = "", documentId = documentId, status = "FAILED", error = e.message))
        }
    }

    @PostMapping("/documents/{documentId}/regenerate")
    fun regenerateFlashcardsForDocument(@PathVariable documentId: String): ResponseEntity<FlashcardApiResponse> {
        return try {
            logger.info("POST /api/flashcards/documents/{}/regenerate", documentId)

            val response = flashcardService.regenerateFlashcards(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(FlashcardApiResponse(flashcards = emptyList(), documentName = "", documentId = documentId, status = "FAILED", error = e.message))
        }
    }
} 