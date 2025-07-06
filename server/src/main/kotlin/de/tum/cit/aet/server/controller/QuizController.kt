package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.QuizGenerationRequest
import de.tum.cit.aet.server.dto.QuizApiResponse
import de.tum.cit.aet.server.service.QuizService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = ["*"])
class QuizController(
    private val quizService: QuizService
) {

    @PostMapping("/generate")
    fun generateQuiz(@RequestBody request: QuizGenerationRequest): ResponseEntity<QuizApiResponse> {
        return try {
            val response = quizService.generateQuiz(request.documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(QuizApiResponse(questions = emptyList(), error = e.message))
        }
    }

    @GetMapping("/documents/{documentId}")
    fun getQuizForDocument(@PathVariable documentId: String): ResponseEntity<QuizApiResponse> {
        return try {
            val response = quizService.generateQuiz(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(QuizApiResponse(questions = emptyList(), error = e.message))
        }
    }
} 