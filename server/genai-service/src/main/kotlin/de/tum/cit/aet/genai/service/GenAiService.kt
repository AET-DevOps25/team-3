package de.tum.cit.aet.genai.service

import de.tum.cit.aet.genai.dto.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.core.ParameterizedTypeReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class GenAiService(
    private val restTemplate: RestTemplate,
    @Value("\${genai.backend.url}")
    private val genaiBackendUrl: String
) {
    
    private val logger = LoggerFactory.getLogger(GenAiService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val objectMapper = ObjectMapper()
    
    private val documentServiceUrl = "http://document-service:8084"

    fun processDocumentAsync(request: ProcessDocumentRequest, callback: (Map<String, Any>?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Processing document: {} for user: {} with type: {}", request.documentId, request.userId, request.processingType)
                
                // Step 1: Get the document's base64 content from the document-service
                val fileInfo = restTemplate.exchange(
                    "$documentServiceUrl/api/documents/internal/${request.documentId}/file",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<Map<String, String>>() {}
                ).body!!

                val fileContent = fileInfo["fileContent"] ?: ""
                val fileName = fileInfo["fileName"] ?: "document.pdf"

                // Step 2: Load the document in the Python GenAI worker
                val sessionRequest = mapOf(
                    "user_id" to request.userId,
                    "document_name" to fileName,
                    "document_base64" to fileContent
                )

                restTemplate.postForEntity(
                    "$genaiBackendUrl/document",
                    sessionRequest,
                    Void::class.java
                )

                logger.info("Session loaded successfully for document: {}", request.documentId)

                // Step 3: Trigger the actual processing based on the type
                when (request.processingType) {
                    ProcessingType.SUMMARY -> {
                        processSummaryOnly(request.documentId, fileName, request.userId, callback)
                    }
                    ProcessingType.QUIZ -> {
                        processQuizOnly(request.documentId, request.userId, fileName, callback)
                    }
                    ProcessingType.FLASHCARD -> {
                        processFlashcardsOnly(request.documentId, request.userId, fileName, callback)
                    }
                    ProcessingType.ALL -> {
                        processAllContentTypes(request.documentId, fileName, request.userId, callback)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing document: {}", e.message, e)
                callback(null, e)
            }
        }
    }
    
    fun getProcessingStatus(requestId: String, userId: String): ProcessingStatusResponse {
        logger.info("Getting processing status for request: {} and user: {}", requestId, userId)
        
        return try {
            val response = restTemplate.getForObject(
                "$genaiBackendUrl/status/$requestId",
                Map::class.java
            )!!
            
            ProcessingStatusResponse(
                requestId = requestId,
                status = ProcessingStatus.valueOf(response["status"] as String),
                progress = (response["progress"] as? Number)?.toInt() ?: 0,
                result = null,
                error = response["error"] as? String
            )
        } catch (e: Exception) {
            logger.error("Error getting processing status: {}", e.message, e)
            ProcessingStatusResponse(
                requestId = requestId,
                status = ProcessingStatus.FAILED,
                progress = 0,
                error = "Failed to get status: ${e.message}"
            )
        }
    }
    
    fun chat(request: ChatRequest): ChatResponse {
        logger.info("Processing chat request for user: {}", request.userId)
        
        return try {
            val userId = request.userId ?: UUID.randomUUID().toString()
            val chatRequest = mapOf(
                "message" to request.message,
                "user_id" to userId
            )
            
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/chat",
                chatRequest,
                Map::class.java
            )!!
            
            ChatResponse(
                response = response["response"] as String,
                sessionId = userId,
                timestamp = LocalDateTime.now().format(dateFormatter)
            )
        } catch (e: Exception) {
            logger.error("Error processing chat request: {}", e.message, e)
            ChatResponse(
                response = "Sorry, I encountered an error processing your request.",
                sessionId = request.userId ?: UUID.randomUUID().toString(),
                timestamp = LocalDateTime.now().format(dateFormatter)
            )
        }
    }
    
    fun chatAsync(request: ChatRequest, callback: (ChatResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Processing async chat request for user: {}", request.userId)
                
                val userId = request.userId ?: UUID.randomUUID().toString()
                val chatRequest = mapOf(
                    "message" to request.message,
                    "user_id" to userId
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/chat",
                    chatRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw chat response: {}", response)
                
                callback(ChatResponse(
                    response = response["response"] as String,
                    sessionId = userId,
                    timestamp = LocalDateTime.now().format(dateFormatter)
                ), null)
            } catch (e: Exception) {
                logger.error("Error processing async chat request: {}", e.message, e)
                callback(ChatResponse(
                    response = "Sorry, I encountered an error processing your request.",
                    sessionId = request.userId ?: UUID.randomUUID().toString(),
                    timestamp = LocalDateTime.now().format(dateFormatter)
                ), e)
            }
        }
    }

    fun createSession(request: CreateSessionRequest): GenAiResponse {
        logger.info("Creating session: {} for document: {}", request.sessionId, request.documentName)
        
        return try {
            val sessionRequest = mapOf(
                "user_id" to request.sessionId,
                "document_name" to request.documentName,
                "document_content" to request.documentBase64
            )
            
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/document",
                sessionRequest,
                Map::class.java
            )!!
            
            GenAiResponse(response = "Session created successfully")
        } catch (e: Exception) {
            logger.error("Error creating session: {}", e.message, e)
            GenAiResponse(response = "Failed to create session: ${e.message}")
        }
    }

    fun createSessionAsync(request: CreateSessionRequest, callback: (GenAiResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Creating session: {} for document: {}", request.sessionId, request.documentName)
                
                val sessionRequest = mapOf(
                    "user_id" to request.sessionId,
                    "document_name" to request.documentName,
                    "document_base64" to request.documentBase64
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/document",
                    sessionRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw session creation response: {}", response)
                
                callback(GenAiResponse(response = "Session created successfully"), null)
            } catch (e: Exception) {
                logger.error("Error creating session: {}", e.message, e)
                callback(GenAiResponse(response = "Failed to create session: ${e.message}"), e)
            }
        }
    }

    fun getSession(userId: String): GenAiResponse {
        logger.info("Getting session: {} for user: {}", userId, userId)
        
        return try {
            val response = restTemplate.getForObject(
                "$genaiBackendUrl/sessions/$userId",
                Map::class.java
            )!!
            
            GenAiResponse(response = response["session_data"] as? String ?: "Session not found")
        } catch (e: Exception) {
            logger.error("Error getting session: {}", e.message, e)
            GenAiResponse(response = "Failed to get session: ${e.message}")
        }
    }

    fun getSessionAsync(userId: String, callback: (GenAiResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Getting session for user: {}", userId)
                
                // Python backend doesn't have a GET session endpoint, so we just return success if session exists
                // We could check health endpoint or just assume session exists since this is mainly for compatibility
                logger.warn("Get session endpoint not implemented in Python backend, returning success for user: {}", userId)
                
                callback(GenAiResponse(response = "Session active for user: $userId"), null)
            } catch (e: Exception) {
                logger.error("Error getting session: {}", e.message, e)
                callback(GenAiResponse(response = "Failed to get session: ${e.message}"), e)
            }
        }
    }

    fun addMessage(sessionId: String, request: PromptRequest): GenAiMessageResponse {
        logger.info("Adding message to session: {}", sessionId)
        
        return try {
            val messageRequest = mapOf(
                "user_id" to sessionId,
                "message" to request.message
            )
            
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/sessions/$sessionId/messages",
                messageRequest,
                Map::class.java
            )!!
            
            GenAiMessageResponse(message = response["response"] as? String ?: "No response")
        } catch (e: Exception) {
            logger.error("Error adding message: {}", e.message, e)
            GenAiMessageResponse(message = "Failed to add message: ${e.message}")
        }
    }

    fun addMessageAsync(sessionId: String, request: PromptRequest, callback: (GenAiMessageResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Adding message to session: {}", sessionId)
                
                // Use the chat endpoint since sessions/$sessionId/messages doesn't exist in Python backend
                val messageRequest = mapOf(
                    "user_id" to sessionId,
                    "message" to request.message
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/chat",
                    messageRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw message response: {}", response)
                
                callback(GenAiMessageResponse(message = response["response"] as? String ?: "No response"), null)
            } catch (e: Exception) {
                logger.error("Error adding message: {}", e.message, e)
                callback(GenAiMessageResponse(message = "Failed to add message: ${e.message}"), e)
            }
        }
    }
    
    // Quiz methods
    fun generateQuiz(request: QuizGenerationRequest): QuizApiResponse {
        logger.info("Generating quiz for user: {}", request.userId)
        
        return try {
            val quizRequest = mapOf(
                "user_id" to request.userId,
                "document_id" to request.documentId,
                "num_questions" to request.numQuestions
            )
            
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/quiz/generate",
                quizRequest,
                Map::class.java
            )!!
            
            QuizApiResponse(
                quiz = response["quiz"] as? Map<String, Any> ?: emptyMap(),
                documentName = response["document_name"] as? String ?: "Unknown",
                documentId = request.documentId,
                status = "success"
            )
        } catch (e: Exception) {
            logger.error("Error generating quiz: {}", e.message, e)
            QuizApiResponse(
                quiz = emptyMap(),
                documentName = "Unknown",
                documentId = request.documentId,
                status = "error",
                error = e.message
            )
        }
    }

    fun generateQuizAsync(request: QuizGenerationRequest, callback: (QuizApiResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Generating quiz for user: {}", request.userId)
                
                val quizRequest = mapOf(
                    "user_id" to request.userId,
                    "document_name" to "document"  // Python backend expects document_name
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/quiz",  // Changed from /quiz/generate to /quiz
                    quizRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw quiz response: {}", response)
                
                // Parse the nested response structure: {"response": {"questions": [...]}}
                val responseData = response["response"] as? Map<String, Any> ?: emptyMap()
                
                callback(QuizApiResponse(
                    quiz = responseData,
                    documentName = "Generated Quiz",  // Python backend doesn't return document name
                    documentId = request.documentId,
                    status = "success"
                ), null)
            } catch (e: Exception) {
                logger.error("Error generating quiz: {}", e.message, e)
                callback(QuizApiResponse(
                    quiz = emptyMap(),
                    documentName = "Unknown",
                    documentId = request.documentId,
                    status = "error",
                    error = e.message
                ), e)
            }
        }
    }

    fun getQuizForDocument(documentId: String, userId: String): QuizApiResponse {
        logger.info("Getting quiz for document: {} and user: {}", documentId, userId)
        
        return try {
            val response = restTemplate.getForObject(
                "$genaiBackendUrl/quiz/documents/$documentId",
                Map::class.java
            )!!
            
            QuizApiResponse(
                quiz = response["quiz"] as? Map<String, Any> ?: emptyMap(),
                documentName = response["document_name"] as? String ?: "Unknown",
                documentId = documentId,
                status = "success"
            )
        } catch (e: Exception) {
            logger.error("Error getting quiz: {}", e.message, e)
            QuizApiResponse(
                quiz = emptyMap(),
                documentName = "Unknown",
                documentId = documentId,
                status = "error",
                error = e.message
            )
        }
    }

    fun getQuizForDocumentAsync(documentId: String, userId: String, callback: (QuizApiResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Getting quiz for document: {} and user: {}", documentId, userId)
                
                // Since Python backend doesn't persist quiz data, we regenerate it
                val quizRequest = mapOf(
                    "user_id" to userId,
                    "document_name" to "document"
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/quiz",
                    quizRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw quiz response for document retrieval: {}", response)
                
                val responseData = response["response"] as? Map<String, Any> ?: emptyMap()
                
                callback(QuizApiResponse(
                    quiz = responseData,
                    documentName = "Generated Quiz",
                    documentId = documentId,
                    status = "success"
                ), null)
            } catch (e: Exception) {
                logger.error("Error getting quiz: {}", e.message, e)
                callback(QuizApiResponse(
                    quiz = emptyMap(),
                    documentName = "Unknown",
                    documentId = documentId,
                    status = "error",
                    error = e.message
                ), e)
            }
        }
    }
    
    fun regenerateQuiz(documentId: String, userId: String): QuizApiResponse {
        logger.info("Regenerating quiz for document: {} and user: {}", documentId, userId)
        
        return try {
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/quiz/documents/$documentId/regenerate",
                null, // No body for regenerate
                Map::class.java
            )!!
            
            QuizApiResponse(
                quiz = response["quiz"] as? Map<String, Any> ?: emptyMap(),
                documentName = response["document_name"] as? String ?: "Unknown",
                documentId = documentId,
                status = "success"
            )
        } catch (e: Exception) {
            logger.error("Error regenerating quiz: {}", e.message, e)
            QuizApiResponse(
                quiz = emptyMap(),
                documentName = "Unknown",
                documentId = documentId,
                status = "error",
                error = e.message
            )
        }
    }

    fun regenerateQuizAsync(documentId: String, userId: String, callback: (QuizApiResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Regenerating quiz for document: {} and user: {}", documentId, userId)
                
                // Regenerate by calling the quiz endpoint again
                val quizRequest = mapOf(
                    "user_id" to userId,
                    "document_name" to "document"
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/quiz",
                    quizRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw quiz regeneration response: {}", response)
                
                val responseData = response["response"] as? Map<String, Any> ?: emptyMap()
                
                callback(QuizApiResponse(
                    quiz = responseData,
                    documentName = "Regenerated Quiz",
                    documentId = documentId,
                    status = "success"
                ), null)
            } catch (e: Exception) {
                logger.error("Error regenerating quiz: {}", e.message, e)
                callback(QuizApiResponse(
                    quiz = emptyMap(),
                    documentName = "Unknown",
                    documentId = documentId,
                    status = "error",
                    error = e.message
                ), e)
            }
        }
    }
    
    // Summary methods
    private fun generateSummary(documentId: String, fileName: String, userId: String): SummaryResponse {
        try {
            logger.info("Request sent to generate summary for document: {}", documentId)
            val summaryRequest = mapOf(
                "user_id" to userId,
                "document_name" to fileName
            )
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/summary",
                summaryRequest,
                GenAiResponse::class.java
            )

            logger.info("Received response from Python service: {}", response)

            if (response == null) {
                logger.error("Null response received from Python service")
                throw RuntimeException("Null response received from Python service")
            }

            // Check if the response contains an error
            if (response.response.startsWith("ERROR:")) {
                logger.error("Error response from Python service: {}", response.response)
                return SummaryResponse(
                    status = "error",
                    summary = null,
                    error = response.response.removePrefix("ERROR: ")
                )
            }

            return SummaryResponse(
                status = "success",
                summary = response.response,
                error = null
            )
        } catch (e: Exception) {
            logger.error("Error generating summary: {}", e.message, e)
            return SummaryResponse(
                status = "error",
                summary = null,
                error = e.message ?: "Unknown error"
            )
        }
    }

    @Async
    fun generateSummaryAsync(request: SummaryGenerationRequest, callback: (SummaryResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                 val fileInfo = restTemplate.exchange(
                    "$documentServiceUrl/api/documents/internal/${request.documentId}/file",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<Map<String, String>>() {}
                ).body!!

                val fileContent = fileInfo["fileContent"] ?: ""
                val fileName = fileInfo["fileName"] ?: "document.pdf"

                val sessionRequest = mapOf(
                    "user_id" to request.userId,
                    "document_name" to fileName,
                    "document_base64" to fileContent
                )

                restTemplate.postForEntity(
                    "$genaiBackendUrl/document",
                    sessionRequest,
                    Void::class.java
                )
                logger.info("Session loaded successfully for document: {}", request.documentId)

                val summaryResponse = generateSummary(request.documentId, fileName, request.userId)
                if (summaryResponse.status == "error") {
                    callback(summaryResponse, RuntimeException(summaryResponse.error))
                } else {
                    callback(summaryResponse, null)
                }
            } catch (e: Exception) {
                logger.error("Error in generateSummaryAsync: {}", e.message, e)
                val errorResponse = SummaryResponse(
                    summary = "",
                    documentId = request.documentId,
                    status = "error",
                    error = e.message
                )
                callback(errorResponse, e)
            }
        }
    }

    // Flashcard methods
    fun generateFlashcardsAsync(request: FlashcardRequest, callback: (FlashcardResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Generating flashcards for session: {}", request.sessionId)
                
                val flashcardRequest = mapOf(
                    "user_id" to request.sessionId,
                    "document_name" to "document"
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/flashcard",  // Changed from /flashcards/generate to /flashcard
                    flashcardRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw flashcard response: {}", response)
                
                // Parse the nested response structure: {"response": {"flashcards": [...]}}
                val responseData = response["response"] as? Map<String, Any>
                val flashcardsData = (responseData?.get("flashcards") as? List<Map<String, Any>> ?: emptyList()).map { card ->
                    FlashcardModel(
                        question = card["question"] as? String ?: "",
                        answer = card["answer"] as? String ?: "",
                        difficulty = card["difficulty"] as? String ?: "medium"
                    )
                }
                callback(FlashcardResponse(FlashcardsData(flashcards = flashcardsData)), null)
            } catch (e: Exception) {
                logger.error("Error generating flashcards: {}", e.message, e)
                callback(FlashcardResponse(FlashcardsData(flashcards = emptyList())), e)
            }
        }
    }

    fun getFlashcardsForDocumentAsync(documentId: String, userId: String, callback: (FlashcardResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Getting flashcards for document: {} and user: {}", documentId, userId)
                
                // Since Python backend doesn't persist flashcard data, we regenerate it
                val flashcardRequest = mapOf(
                    "user_id" to userId,
                    "document_name" to "document"
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/flashcard",
                    flashcardRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw flashcard response for document retrieval: {}", response)
                
                val responseData = response["response"] as? Map<String, Any>
                val flashcardsData = (responseData?.get("flashcards") as? List<Map<String, Any>> ?: emptyList()).map { card ->
                    FlashcardModel(
                        question = card["question"] as? String ?: "",
                        answer = card["answer"] as? String ?: "",
                        difficulty = card["difficulty"] as? String ?: "medium"
                    )
                }
                callback(FlashcardResponse(FlashcardsData(flashcards = flashcardsData)), null)
            } catch (e: Exception) {
                logger.error("Error getting flashcards: {}", e.message, e)
                callback(FlashcardResponse(FlashcardsData(flashcards = emptyList())), e)
            }
        }
    }

    fun regenerateFlashcardsAsync(documentId: String, userId: String, callback: (FlashcardResponse?, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Regenerating flashcards for document: {} and user: {}", documentId, userId)
                
                // Regenerate by calling the flashcard endpoint again
                val flashcardRequest = mapOf(
                    "user_id" to userId,
                    "document_name" to "document"
                )
                
                val response = restTemplate.postForObject(
                    "$genaiBackendUrl/flashcard",
                    flashcardRequest,
                    Map::class.java
                )!!
                
                logger.info("Raw flashcard regeneration response: {}", response)
                
                val responseData = response["response"] as? Map<String, Any>
                val flashcardsData = (responseData?.get("flashcards") as? List<Map<String, Any>> ?: emptyList()).map { card ->
                    FlashcardModel(
                        question = card["question"] as? String ?: "",
                        answer = card["answer"] as? String ?: "",
                        difficulty = card["difficulty"] as? String ?: "medium"
                    )
                }
                callback(FlashcardResponse(FlashcardsData(flashcards = flashcardsData)), null)
            } catch (e: Exception) {
                logger.error("Error regenerating flashcards: {}", e.message, e)
                callback(FlashcardResponse(FlashcardsData(flashcards = emptyList())), e)
            }
        }
    }

    // Helper methods for processing different content types
    private fun processSummaryOnly(documentId: String, fileName: String, userId: String, callback: (Map<String, Any>?, Exception?) -> Unit) {
        try {
            logger.info("Triggering summary generation for document: {}", documentId)
            val summaryResponse = generateSummary(documentId, fileName, userId)

            logger.info("Received response from generateSummary: {}", summaryResponse)

            if (summaryResponse.status == "error") {
                logger.error("Summary generation failed with error: {}", summaryResponse.error)
                throw RuntimeException(summaryResponse.error ?: "Unknown error")
            }

            if (summaryResponse.summary.isNullOrBlank()) {
                logger.error("Empty summary received")
                throw RuntimeException("Empty summary received from backend")
            }

            // Update the document with the summary
            val summaryUpdateRequest = SummaryUpdateRequest(summary = summaryResponse.summary)
            logger.info("Sending summary update request to document-service: {}", summaryUpdateRequest)

            val documentResponse = restTemplate.exchange(
                "$documentServiceUrl/api/documents/internal/${documentId}/summary",
                HttpMethod.PUT,
                HttpEntity(summaryUpdateRequest),
                String::class.java
            )

            if (documentResponse.statusCode.is2xxSuccessful) {
                logger.info("Document summary updated successfully for document: {}", documentId)
                callback(mapOf(
                    "status" to "COMPLETED",
                    "message" to "Summary generated and saved.",
                    "summary" to summaryResponse.summary
                ), null)
            } else {
                val error = "Failed to update document summary: ${documentResponse.statusCode}"
                logger.error(error)
                throw RuntimeException(error)
            }
        } catch (e: Exception) {
            logger.error("Error processing summary: {}", e.message, e)
            callback(null, e)
        }
    }

    private fun processQuizOnly(documentId: String, userId: String, fileName: String, callback: (Map<String, Any>?, Exception?) -> Unit) {
        try {
            logger.info("Triggering quiz generation for document: {}", documentId)
            
            val quizRequest = mapOf(
                "user_id" to userId,
                "document_name" to fileName
            )
            
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/quiz",
                quizRequest,
                Map::class.java
            )!!
            
            logger.info("Raw quiz response: {}", response)
            
            val responseData = response["response"] as? Map<String, Any> ?: emptyMap()
            
            callback(mapOf(
                "status" to "COMPLETED",
                "message" to "Quiz generated successfully.",
                "quiz" to responseData
            ), null)
        } catch (e: Exception) {
            logger.error("Error processing quiz: {}", e.message, e)
            callback(null, e)
        }
    }

    private fun processFlashcardsOnly(documentId: String, userId: String, fileName: String, callback: (Map<String, Any>?, Exception?) -> Unit) {
        try {
            logger.info("Triggering flashcard generation for document: {}", documentId)
            
            val flashcardRequest = mapOf(
                "user_id" to userId,
                "document_name" to fileName
            )
            
            val response = restTemplate.postForObject(
                "$genaiBackendUrl/flashcard",
                flashcardRequest,
                Map::class.java
            )!!
            
            logger.info("Raw flashcard response: {}", response)
            
            val responseData = response["response"] as? Map<String, Any>
            val flashcardsData = (responseData?.get("flashcards") as? List<Map<String, Any>> ?: emptyList()).map { card ->
                mapOf(
                    "question" to (card["question"] as? String ?: ""),
                    "answer" to (card["answer"] as? String ?: ""),
                    "difficulty" to (card["difficulty"] as? String ?: "medium")
                )
            }
            
            callback(mapOf(
                "status" to "COMPLETED",
                "message" to "Flashcards generated successfully.",
                "flashcards" to flashcardsData
            ), null)
        } catch (e: Exception) {
            logger.error("Error processing flashcards: {}", e.message, e)
            callback(null, e)
        }
    }

    private fun processAllContentTypes(documentId: String, fileName: String, userId: String, callback: (Map<String, Any>?, Exception?) -> Unit) {
        try {
            logger.info("Triggering parallel generation of all content types for document: {}", documentId)
            
            // Create CompletableFutures for parallel execution
            val summaryFuture = CompletableFuture.supplyAsync {
                try {
                    logger.info("Starting summary generation in parallel for document: {}", documentId)
                    val summaryResponse = generateSummary(documentId, fileName, userId)
                    if (summaryResponse.status == "error") {
                        throw RuntimeException(summaryResponse.error ?: "Summary generation failed")
                    }
                    logger.info("Summary generation completed for document: {}", documentId)
                    summaryResponse
                } catch (e: Exception) {
                    logger.error("Summary generation failed in parallel processing: {}", e.message, e)
                    throw e
                }
            }
            
            val quizFuture = CompletableFuture.supplyAsync {
                try {
                    logger.info("Starting quiz generation in parallel for document: {}", documentId)
                    val quizRequest = mapOf("user_id" to userId, "document_name" to fileName)
                    val response = restTemplate.postForObject("$genaiBackendUrl/quiz", quizRequest, Map::class.java)!!
                    logger.info("Quiz generation completed for document: {}", documentId)
                    response["response"] as? Map<String, Any> ?: emptyMap()
                } catch (e: Exception) {
                    logger.error("Quiz generation failed in parallel processing: {}", e.message, e)
                    throw e
                }
            }
            
            val flashcardFuture = CompletableFuture.supplyAsync {
                try {
                    logger.info("Starting flashcard generation in parallel for document: {}", documentId)
                    val flashcardRequest = mapOf("user_id" to userId, "document_name" to fileName)
                    val response = restTemplate.postForObject("$genaiBackendUrl/flashcard", flashcardRequest, Map::class.java)!!
                    val responseData = response["response"] as? Map<String, Any>
                    val flashcardsData = (responseData?.get("flashcards") as? List<Map<String, Any>> ?: emptyList()).map { card ->
                        mapOf(
                            "question" to (card["question"] as? String ?: ""),
                            "answer" to (card["answer"] as? String ?: ""),
                            "difficulty" to (card["difficulty"] as? String ?: "medium")
                        )
                    }
                    logger.info("Flashcard generation completed for document: {}", documentId)
                    flashcardsData
                } catch (e: Exception) {
                    logger.error("Flashcard generation failed in parallel processing: {}", e.message, e)
                    throw e
                }
            }
            
            // Wait for all tasks to complete
            val allResults = CompletableFuture.allOf(summaryFuture, quizFuture, flashcardFuture)
            
            allResults.thenRun {
                try {
                    val summaryResponse = summaryFuture.get()
                    val quizData = quizFuture.get()
                    val flashcardsData = flashcardFuture.get()
                    
                    logger.info("All content types generated successfully for document: {}", documentId)
                    
                    // Update the document with all generated content
                    var summaryUpdated = false
                    var quizUpdated = false
                    var flashcardsUpdated = false
                    
                    // Update summary
                    if (!summaryResponse.summary.isNullOrBlank()) {
                        try {
                            val summaryUpdateRequest = SummaryUpdateRequest(summary = summaryResponse.summary)
                            logger.info("Sending summary update request to document-service: {}", summaryUpdateRequest)

                            val documentResponse = restTemplate.exchange(
                                "$documentServiceUrl/api/documents/internal/${documentId}/summary",
                                HttpMethod.PUT,
                                HttpEntity(summaryUpdateRequest),
                                String::class.java
                            )

                            if (documentResponse.statusCode.is2xxSuccessful) {
                                logger.info("Document summary updated successfully for document: {}", documentId)
                                summaryUpdated = true
                            } else {
                                logger.warn("Failed to update document summary: {}", documentResponse.statusCode)
                            }
                        } catch (e: Exception) {
                            logger.error("Error updating summary: {}", e.message, e)
                        }
                    }
                    
                    // Update quiz data
                    if (quizData.isNotEmpty()) {
                        try {
                            logger.info("Sending quiz update request to document-service for document: {}", documentId)

                            val quizResponse = restTemplate.exchange(
                                "$documentServiceUrl/api/documents/internal/${documentId}/quiz",
                                HttpMethod.PUT,
                                HttpEntity(quizData),
                                String::class.java
                            )

                            if (quizResponse.statusCode.is2xxSuccessful) {
                                logger.info("Document quiz updated successfully for document: {}", documentId)
                                quizUpdated = true
                            } else {
                                logger.warn("Failed to update document quiz: {}", quizResponse.statusCode)
                            }
                        } catch (e: Exception) {
                            logger.error("Error updating quiz: {}", e.message, e)
                        }
                    }
                    
                    // Update flashcard data
                    if (flashcardsData.isNotEmpty()) {
                        try {
                            logger.info("Sending flashcards update request to document-service for document: {}", documentId)

                            val flashcardsResponse = restTemplate.exchange(
                                "$documentServiceUrl/api/documents/internal/${documentId}/flashcards",
                                HttpMethod.PUT,
                                HttpEntity(flashcardsData),
                                String::class.java
                            )

                            if (flashcardsResponse.statusCode.is2xxSuccessful) {
                                logger.info("Document flashcards updated successfully for document: {}", documentId)
                                flashcardsUpdated = true
                            } else {
                                logger.warn("Failed to update document flashcards: {}", flashcardsResponse.statusCode)
                            }
                        } catch (e: Exception) {
                            logger.error("Error updating flashcards: {}", e.message, e)
                        }
                    }
                    
                    // Return all generated content with update status
                    val updatedContentTypes = mutableListOf<String>()
                    if (summaryUpdated) updatedContentTypes.add("summary")
                    if (quizUpdated) updatedContentTypes.add("quiz") 
                    if (flashcardsUpdated) updatedContentTypes.add("flashcards")
                    
                    val statusMessage = if (updatedContentTypes.isNotEmpty()) {
                        "Content generated and saved: ${updatedContentTypes.joinToString(", ")}"
                    } else {
                        "Content generated but failed to save to database"
                    }
                    
                    callback(mapOf(
                        "status" to "COMPLETED",
                        "message" to statusMessage,
                        "summary" to (summaryResponse.summary ?: ""),
                        "quiz" to quizData,
                        "flashcards" to flashcardsData,
                        "contentTypes" to updatedContentTypes,
                        "updatedContentTypes" to updatedContentTypes,
                        "summaryUpdated" to summaryUpdated,
                        "quizUpdated" to quizUpdated,
                        "flashcardsUpdated" to flashcardsUpdated
                    ), null)
                } catch (e: Exception) {
                    logger.error("Error in parallel processing completion: {}", e.message, e)
                    callback(null, e)
                }
            }.exceptionally { throwable ->
                logger.error("Error in parallel processing: {}", throwable.message, throwable)
                callback(null, Exception(throwable))
                null
            }
            
        } catch (e: Exception) {
            logger.error("Error starting parallel processing: {}", e.message, e)
            callback(null, e)
        }
    }

    // New method to load document for chat only (no content generation)
    fun loadDocumentForChatAsync(documentId: String, userId: String, callback: (Boolean, Exception?) -> Unit) {
        CompletableFuture.runAsync {
            try {
                logger.info("Loading document {} for chat only (user: {})", documentId, userId)
                
                // Step 1: Get the document's base64 content from the document-service
                val fileInfo = restTemplate.exchange(
                    "$documentServiceUrl/api/documents/internal/${documentId}/file",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<Map<String, String>>() {}
                ).body!!

                val fileContent = fileInfo["fileContent"] ?: ""
                val fileName = fileInfo["fileName"] ?: "document.pdf"

                // Step 2: Load the document in Python for this user (no content generation)
                val sessionRequest = mapOf(
                    "user_id" to userId,
                    "document_name" to fileName,
                    "document_base64" to fileContent
                )

                restTemplate.postForEntity(
                    "$genaiBackendUrl/document",
                    sessionRequest,
                    Void::class.java
                )

                logger.info("Document {} loaded successfully for chat (user: {})", documentId, userId)
                callback(true, null)
                
            } catch (e: Exception) {
                logger.error("Error loading document {} for chat: {}", documentId, e.message, e)
                callback(false, e)
            }
        }
    }


} 