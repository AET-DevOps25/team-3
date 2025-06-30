package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.*

@Service
class GenAiService(
    private val webClientBuilder: WebClient.Builder
) {
    
    @Value("\${genai.service.url:http://localhost:8081}")
    private lateinit var genAiBaseUrl: String
    
    private val webClient by lazy {
        webClientBuilder
            .baseUrl(genAiBaseUrl)
            .build()
    }
    
    fun createSession(documentId: String, documentName: String, documentContent: ByteArray): String? {
        return try {
            val base64Content = Base64.getEncoder().encodeToString(documentContent)
            val request = CreateSessionRequest(
                sessionId = documentId,
                documentName = documentName,
                documentBase64 = base64Content
            )
            
            val response = webClient
                .post()
                .uri("/session/load")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenAiMessageResponse::class.java)
                .block()
            
            response?.message
        } catch (e: WebClientResponseException) {
            println("Error creating GenAI session: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error creating GenAI session: ${e.message}")
            null
        }
    }
    
    fun generateSummary(documentId: String): String? {
        return try {
            val request = SummaryRequest(sessionId = documentId)
            
            val response = webClient
                .post()
                .uri("/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenAiResponse::class.java)
                .block()
            
            response?.response
        } catch (e: WebClientResponseException) {
            println("Error generating summary: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error generating summary: ${e.message}")
            null
        }
    }
    
    fun chatWithDocument(documentId: String, message: String): String? {
        return try {
            val request = PromptRequest(
                sessionId = documentId,
                message = message
            )
            
            val response = webClient
                .post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenAiResponse::class.java)
                .block()
            
            response?.response
        } catch (e: WebClientResponseException) {
            println("Error in chat: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error in chat: ${e.message}")
            null
        }
    }
    
    fun generateFlashcards(documentId: String): List<FlashcardModel>? {
        return try {
            val request = FlashcardRequest(sessionId = documentId)
            
            val response = webClient
                .post()
                .uri("/flashcard")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FlashcardResponse::class.java)
                .block()
            
            response?.response?.flashcards
        } catch (e: WebClientResponseException) {
            println("Error generating flashcards: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error generating flashcards: ${e.message}")
            null
        }
    }
    
    fun generateQuiz(documentId: String): List<QuestionModel>? {
        return try {
            val request = QuizRequest(sessionId = documentId)
            
            val response = webClient
                .post()
                .uri("/quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QuizResponse::class.java)
                .block()
            
            response?.response?.questions
        } catch (e: Exception) {
            println("Error generating quiz: ${e.message}")
            null
        }
    }
    
    fun healthCheck(): Boolean {
        return try {
            val response = webClient
                .get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
            
            response?.get("status") == "healthy"
        } catch (e: Exception) {
            println("GenAI service health check failed: ${e.message}")
            false
        }
    }
} 