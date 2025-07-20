package de.tum.cit.aet.genai.controller

import de.tum.cit.aet.genai.dto.*
import de.tum.cit.aet.genai.service.GenAiService
import de.tum.cit.aet.genai.service.ChatMessageService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.async.DeferredResult

@RestController
@RequestMapping("/api/genai")
@CrossOrigin(origins = ["*"])
class GenAiController(
    private val genAiService: GenAiService,
    private val chatMessageService: ChatMessageService
) {
    
    private val logger = LoggerFactory.getLogger(GenAiController::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                error = "Validation failed",
                message = "Request validation failed: $errors",
                timestamp = LocalDateTime.now().format(dateFormatter)
            )
        )
    }
    
    @PostMapping("/process")
    fun processDocument(
        @Valid @RequestBody request: ProcessDocumentRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/process - document: {} for user: {}", request.documentId, request.userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.processDocumentAsync(request) { response, error ->
            if (error != null) {
                logger.error("Error processing document: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Processing failed",
                        message = "Failed to process document: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @GetMapping("/status/{requestId}")
    fun getProcessingStatus(
        @PathVariable requestId: String,
        @RequestHeader("X-User-ID") userId: String
    ): ResponseEntity<Any> {
        logger.info("GET /genai/status/{} for user: {}", requestId, userId)
        
        return try {
            val response = genAiService.getProcessingStatus(requestId, userId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting processing status: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(
                    error = "Status check failed",
                    message = "Failed to get processing status: ${e.message}",
                    timestamp = LocalDateTime.now().format(dateFormatter)
                )
            )
        }
    }
    
    @PostMapping("/chat")
    fun chat(
        @Valid @RequestBody request: ChatRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/chat for user: {}", request.userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.chatAsync(request) { response, error ->
            if (error != null) {
                logger.error("Error processing chat request: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Chat failed",
                        message = "Failed to process chat request: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @PostMapping("/sessions")
    fun createSession(
        @Valid @RequestBody request: CreateSessionRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/sessions for user: {}", request.sessionId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.createSessionAsync(request) { response, error ->
            if (error != null) {
                logger.error("Error creating session: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Session creation failed",
                        message = "Failed to create session: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @GetMapping("/sessions/{sessionId}")
    fun getSession(
        @PathVariable sessionId: String,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("GET /genai/sessions/{} for user: {}", sessionId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.getSessionAsync(userId) { response, error ->
            if (error != null) {
                logger.error("Error getting session: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Session retrieval failed",
                        message = "Failed to get session: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @PostMapping("/sessions/{sessionId}/messages")
    fun addMessage(
        @PathVariable sessionId: String,
        @Valid @RequestBody request: PromptRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/sessions/{}/messages for user: {}", sessionId, request.sessionId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.addMessageAsync(sessionId, request) { response, error ->
            if (error != null) {
                logger.error("Error adding message: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Message addition failed",
                        message = "Failed to add message: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @PostMapping("/quiz/generate")
    fun generateQuiz(
        @Valid @RequestBody request: QuizGenerationRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/quiz/generate for user: {}", request.userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.generateQuizAsync(request) { response, error ->
            if (error != null) {
                logger.error("Error generating quiz: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Quiz generation failed",
                        message = "Failed to generate quiz: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @GetMapping("/quiz/documents/{documentId}")
    fun getQuizForDocument(
        @PathVariable documentId: String,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("GET /genai/quiz/documents/{} for user: {}", documentId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.getQuizForDocumentAsync(documentId, userId) { response, error ->
            if (error != null) {
                logger.error("Error getting quiz: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Quiz retrieval failed",
                        message = "Failed to get quiz: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @PostMapping("/quiz/documents/{documentId}/regenerate")
    fun regenerateQuiz(
        @PathVariable documentId: String,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/quiz/documents/{}/regenerate for user: {}", documentId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.regenerateQuizAsync(documentId, userId) { response, error ->
            if (error != null) {
                logger.error("Error regenerating quiz: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Quiz regeneration failed",
                        message = "Failed to regenerate quiz: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }

    @PostMapping("/summary/generate")
    fun generateSummary(
        @Valid @RequestBody request: SummaryGenerationRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/summary/generate for user: {}", request.userId)

        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout

        genAiService.generateSummaryAsync(request) { response, error ->
            if (error != null) {
                logger.error("Error generating summary: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Summary generation failed",
                        message = "Failed to generate summary: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }

        return result
    }
    
    @PostMapping("/flashcards/generate")
    fun generateFlashcards(
        @Valid @RequestBody request: FlashcardRequest
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/flashcards/generate for user: {}", request.sessionId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.generateFlashcardsAsync(request) { response, error ->
            if (error != null) {
                logger.error("Error generating flashcards: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Flashcard generation failed",
                        message = "Failed to generate flashcards: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @GetMapping("/flashcards/documents/{documentId}")
    fun getFlashcardsForDocument(
        @PathVariable documentId: String,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("GET /genai/flashcards/documents/{} for user: {}", documentId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.getFlashcardsForDocumentAsync(documentId, userId) { response, error ->
            if (error != null) {
                logger.error("Error getting flashcards: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Flashcard retrieval failed",
                        message = "Failed to get flashcards: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    @PostMapping("/flashcards/documents/{documentId}/regenerate")
    fun regenerateFlashcards(
        @PathVariable documentId: String,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/flashcards/documents/{}/regenerate for user: {}", documentId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        genAiService.regenerateFlashcardsAsync(documentId, userId) { response, error ->
            if (error != null) {
                logger.error("Error regenerating flashcards: {}", error.message, error)
                result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse(
                        error = "Flashcard regeneration failed",
                        message = "Failed to regenerate flashcards: ${error.message}",
                        timestamp = LocalDateTime.now().format(dateFormatter)
                    )
                ))
            } else {
                result.setResult(ResponseEntity.ok(response))
            }
        }
        
        return result
    }
    
    // Chat session endpoints (simplified to match Python service pattern)
    @PostMapping("/chat/sessions")
    fun createChatSession(
        @Valid @RequestBody request: ChatSessionRequest,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/chat/sessions for user: {} with documents: {}", userId, request.documentIds)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        // Python service doesn't have sessions - it just loads documents per user
        // If documents are provided, load the first one for this user (for chat only, no content generation)
        if (request.documentIds.isNotEmpty()) {
            val primaryDocumentId = request.documentIds.first()
            
            genAiService.loadDocumentForChatAsync(primaryDocumentId, userId) { success, error ->
                if (error != null || !success) {
                    logger.error("Error loading document for chat: {}", error?.message ?: "Unknown error")
                    result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ErrorResponse(
                            error = "Failed to initialize chat",
                            message = "Could not load document for chat: ${error?.message ?: "Unknown error"}",
                            timestamp = LocalDateTime.now().format(dateFormatter)
                        )
                    ))
                } else {
                    // Return session response with actual message history
                    val messages = chatMessageService.getSessionMessages(userId) // sessionId = userId
                    val response = mapOf(
                        "sessionId" to userId,
                        "messages" to messages,
                        "documentsInContext" to request.documentIds
                    )
                    result.setResult(ResponseEntity.ok(response))
                }
            }
        } else {
            // No documents provided, return session with existing message history
            val messages = chatMessageService.getSessionMessages(userId) // sessionId = userId
            val response = mapOf(
                "sessionId" to userId,
                "messages" to messages,
                "documentsInContext" to emptyList<String>()
            )
            result.setResult(ResponseEntity.ok(response))
        }
        
        return result
    }
    
    @GetMapping("/chat/sessions/{sessionId}")
    fun getChatSession(
        @PathVariable sessionId: String,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("GET /genai/chat/sessions/{} for user: {}", sessionId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        try {
            // Get actual message history from database
            val messages = chatMessageService.getSessionMessages(sessionId)
            
            val response = mapOf(
                "sessionId" to sessionId,
                "messages" to messages,
                "documentsInContext" to emptyList<String>() // Would need to extract from message context if needed
            )
            
            result.setResult(ResponseEntity.ok(response))
        } catch (e: Exception) {
            logger.error("Error getting chat session: {}", e.message, e)
            result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(
                    error = "Failed to get chat session",
                    message = "Could not retrieve chat history: ${e.message}",
                    timestamp = LocalDateTime.now().format(dateFormatter)
                )
            ))
        }
        
        return result
    }
    
    @PostMapping("/chat/sessions/{sessionId}/messages")
    fun sendChatMessage(
        @PathVariable sessionId: String,
        @Valid @RequestBody request: SendMessageRequest,
        @RequestHeader("X-User-ID") userId: String
    ): DeferredResult<ResponseEntity<Any>> {
        logger.info("POST /genai/chat/sessions/{}/messages for user: {}", sessionId, userId)
        
        val result = DeferredResult<ResponseEntity<Any>>(600000L) // 10 minutes timeout
        
        try {
            // Save user message to database first
            chatMessageService.saveUserMessage(
                sessionId = sessionId,
                userId = userId,
                content = request.message,
                documentIds = request.documentIds
            )
            
            // Use the simple chat functionality - Python service uses user_id for context
            val chatRequest = ChatRequest(message = request.message, userId = userId)
            
            genAiService.chatAsync(chatRequest) { chatResponse, error ->
                if (error != null) {
                    logger.error("Error sending chat message: {}", error.message, error)
                    result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ErrorResponse(
                            error = "Message sending failed",
                            message = "Failed to send message: ${error.message}",
                            timestamp = LocalDateTime.now().format(dateFormatter)
                        )
                    ))
                } else {
                    try {
                        // Save bot response to database
                        val botMessage = chatMessageService.saveBotMessage(
                            sessionId = sessionId,
                            userId = userId,
                            content = chatResponse!!.response
                        )
                        
                        // Transform to expected response format
                        val messageResponse = mapOf(
                            "id" to (botMessage.id ?: java.util.UUID.randomUUID().toString()),
                            "content" to chatResponse.response,
                            "sender" to "bot",
                            "timestamp" to chatResponse.timestamp,
                            "sources" to null,
                            "documentReferences" to null
                        )
                        result.setResult(ResponseEntity.ok(messageResponse))
                    } catch (e: Exception) {
                        logger.error("Error saving bot message: {}", e.message, e)
                        // Still return the response even if saving failed
                        val messageResponse = mapOf(
                            "id" to java.util.UUID.randomUUID().toString(),
                            "content" to chatResponse!!.response,
                            "sender" to "bot",
                            "timestamp" to chatResponse.timestamp,
                            "sources" to null,
                            "documentReferences" to null
                        )
                        result.setResult(ResponseEntity.ok(messageResponse))
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error saving user message: {}", e.message, e)
            result.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(
                    error = "Message saving failed",
                    message = "Failed to save user message: ${e.message}",
                    timestamp = LocalDateTime.now().format(dateFormatter)
                )
            ))
        }
        
        return result
    }

    @DeleteMapping("/chat/sessions/{sessionId}/messages")
    fun clearChatHistory(
        @PathVariable sessionId: String,
        @RequestHeader("X-User-ID") userId: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info("DELETE /genai/chat/sessions/{}/messages for user: {}", sessionId, userId)
        
        return try {
            val deletedCount = chatMessageService.clearSessionMessages(sessionId)
            
            ResponseEntity.ok(mapOf<String, Any>(
                "message" to "Chat history cleared successfully",
                "deletedMessages" to deletedCount,
                "sessionId" to sessionId,
                "timestamp" to LocalDateTime.now().format(dateFormatter)
            ))
        } catch (e: Exception) {
            logger.error("Error clearing chat history: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf<String, Any>(
                "error" to "Failed to clear chat history",
                "message" to (e.message ?: "Unknown error"),
                "timestamp" to LocalDateTime.now().format(dateFormatter)
            ))
        }
    }


    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "healthy",
            "service" to "genai-service",
            "timestamp" to LocalDateTime.now().format(dateFormatter)
        ))
    }
} 