package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.QuizGenerationRequest
import de.tum.cit.aet.server.dto.QuizApiResponse
import de.tum.cit.aet.server.service.QuizService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = ["*"])
class QuizController(
    private val quizService: QuizService
) {
    
    private val logger = LoggerFactory.getLogger(QuizController::class.java)

    @PostMapping("/generate")
    fun generateQuiz(@RequestBody request: QuizGenerationRequest): ResponseEntity<QuizApiResponse> {
        return try {
            logger.info("POST /api/quiz/generate")

            val response = quizService.generateQuiz(request.documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(QuizApiResponse(questions = emptyList(), error = e.message))
        }
    }

    @GetMapping("/documents/{documentId}")
    fun getQuizForDocument(@PathVariable documentId: String): ResponseEntity<QuizApiResponse> {
        return try {
            logger.info("GET /api/quiz/documents/{}", documentId)

            val response = quizService.generateQuiz(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(QuizApiResponse(questions = emptyList(), error = e.message))
        }
    }

    @PostMapping("/documents/{documentId}/regenerate")
    fun regenerateQuizForDocument(@PathVariable documentId: String): ResponseEntity<QuizApiResponse> {
        return try {
            logger.info("POST /api/quiz/documents/{}/regenerate", documentId)

            val response = quizService.regenerateQuiz(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(QuizApiResponse(questions = emptyList(), error = e.message))
        }
    }
} 