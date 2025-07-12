package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import java.util.*
import reactor.core.publisher.Mono

@Service
class GenAiService(
    private val webClientBuilder: WebClient.Builder
) {
    
    private val logger = LoggerFactory.getLogger(GenAiService::class.java)
    
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
            
            logger.info("Attempting to create GenAI session for document: {}", documentName)
            
            val response = webClient
                .post()
                .uri("/session/load")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError || status.is5xxServerError }) { response ->
                    logger.error("GenAI service returned error status: {}", response.statusCode())
                    response.bodyToMono(String::class.java)
                        .flatMap { errorBody ->
                            logger.error("GenAI service error response: {}", errorBody)
                            Mono.error(WebClientResponseException.create(
                                response.statusCode().value(),
                                "GenAI service error",
                                response.headers().asHttpHeaders(),
                                errorBody.toByteArray(),
                                null
                            ))
                        }
                }
                .bodyToMono(GenAiMessageResponse::class.java)
                .block()
            
            if (response != null) {
                logger.info("Successfully created GenAI session for document: {}", documentName)
                response.message
            } else {
                logger.error("GenAI service returned null response for document: {}", documentName)
                null
            }
        } catch (e: WebClientResponseException) {
            logger.error("GenAI service HTTP error for document {}: {} - {}", documentName, e.statusCode, e.message)
            null
        } catch (e: Exception) {
            logger.error("Error creating GenAI session for document {}: {}", documentName, e.message, e)
            null
        }
    }
    
    fun generateSummary(documentId: String): String? {
        return try {
            val request = SummaryRequest(sessionId = documentId)
            logger.info("request sent to generate summary")
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
            logger.error("Error generating summary: {}", e.message, e)
            null
        } catch (e: Exception) {
            logger.error("Error generating summary: {}", e.message, e)
            null
        }
    }
    
    fun chatWithDocument(documentId: String, message: String): String? {
        return try {
            val request = PromptRequest(
                sessionId = documentId,
                message = message
            )
            logger.info("request sent to chat with  document")

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
            logger.error("Error in chat: {}", e.message, e)
            null
        } catch (e: Exception) {
            logger.error("Error in chat: {}", e.message, e)
            null
        }
    }
    
    fun generateFlashcards(documentId: String): List<FlashcardModel>? {
        return try {
            val request = FlashcardRequest(sessionId = documentId)
            logger.info("request sent to generate flashcards")

            val response = webClient
                .post()
                .uri("/flashcard")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
            
            // Parse the response manually to avoid Jackson constructor issues
            if (response != null) {
                val responseMap = response["response"] as? Map<*, *>
                val flashcardsList = responseMap?.get("flashcards") as? List<*>
                
                if (flashcardsList != null) {
                    flashcardsList.map { flashcardMap ->
                        val fc = flashcardMap as Map<*, *>
                        FlashcardModel(
                            question = fc["question"] as? String ?: "",
                            answer = fc["answer"] as? String ?: "",
                            difficulty = fc["difficulty"] as? String ?: "medium"
                        )
                    }
                } else {
                    logger.warn("No flashcards found in GenAI response")
                    null
                }
            } else {
                logger.warn("GenAI service returned null response for flashcards")
                null
            }
        } catch (e: WebClientResponseException) {
            logger.error("Error generating flashcards: {}", e.message, e)
            null
        } catch (e: Exception) {
            logger.error("Error generating flashcards: {}", e.message, e)
            null
        }
    }
    
    fun generateQuiz(documentId: String): List<QuestionModel>? {
        return try {
            val request = QuizRequest(sessionId = documentId)
            logger.info("request sent to generate quiz")

            val response = webClient
                .post()
                .uri("/quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
            
            // Parse the response manually to avoid Jackson constructor issues
            if (response != null) {
                val responseMap = response["response"] as? Map<*, *>
                val questionsList = responseMap?.get("questions") as? List<*>
                
                if (questionsList != null) {
                    questionsList.map { questionMap ->
                        val q = questionMap as Map<*, *>
                        QuestionModel(
                            type = q["type"] as? String ?: "",
                            question = q["question"] as? String ?: "",
                            correctAnswer = q["correct_answer"] as? String ?: "",
                            points = (q["points"] as? Number)?.toInt() ?: 0,
                            options = (q["options"] as? List<*>)?.map { it.toString() }
                        )
                    }
                } else {
                    logger.warn("No questions found in GenAI response")
                    null
                }
            } else {
                logger.warn("GenAI service returned null response for quiz")
                null
            }
        } catch (e: Exception) {
            logger.error("Error generating quiz: {}", e.message, e)
            null
        }
    }
    
    fun healthCheck(): Boolean {
        return try {
            logger.info("Checking GenAI service health...")
            val response = webClient
                .get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
            
            val isHealthy = response?.get("status") == "healthy"
            logger.info("GenAI service health check result: {}", isHealthy)
            isHealthy
        } catch (e: Exception) {
            logger.error("GenAI service health check failed: {}", e.message, e)
            false
        }
    }
    
    fun isServiceAvailable(): Boolean {
        return try {
            val response = webClient
                .get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Map::class.java)
                .timeout(Duration.ofSeconds(5))
                .block()
            
            response?.get("status") == "healthy"
        } catch (e: Exception) {
            logger.warn("GenAI service not available: {}", e.message)
            false
        }
    }
} 