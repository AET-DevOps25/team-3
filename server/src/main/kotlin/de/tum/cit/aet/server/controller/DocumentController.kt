package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.service.DocumentService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["*"])
class DocumentController(private val documentService: DocumentService) {
    
    private val logger = LoggerFactory.getLogger(DocumentController::class.java)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFiles(@RequestParam("files") files: Array<MultipartFile>): ResponseEntity<Any> {
        logger.info("POST /api/documents/upload")
        return try {
            if (files.isEmpty()) {
                return ResponseEntity.badRequest().body(mapOf("error" to "No files provided"))
            }
            
            val response = documentService.uploadFiles(files)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{documentId}/status")
    fun getDocumentStatus(@PathVariable documentId: String): ResponseEntity<Any> {
        logger.info("GET /api/documents/{}/status", documentId)
        return try {
            val response = documentService.getDocumentStatus(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/{documentId}/content")
    fun getDocumentContent(@PathVariable documentId: String): ResponseEntity<Any> {
        logger.info("GET /api/documents/{}/content", documentId)
        return try {
            val response = documentService.getDocumentContent(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/{documentId}/quiz")
    fun getDocumentQuiz(@PathVariable documentId: String): ResponseEntity<Any> {
        logger.info("GET /api/documents/{}/quiz", documentId)
        return try {
            val documentEntity = documentService.getDocumentEntity(documentId)
                ?: return ResponseEntity.notFound().build()
            
            if (documentEntity.quizData != null) {
                val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                try {
                    // Always parse as Map to avoid Jackson/Kotlin constructor issues
                    val quizDataMap = objectMapper.readValue(documentEntity.quizData.toString(), Map::class.java) as Map<*, *>
                    val responseMap = quizDataMap["response"] as? Map<*, *>
                    val questionsList = responseMap?.get("questions") as? List<*>
                    if (questionsList != null) {
                        val questions = questionsList.map { questionMap ->
                            val q = questionMap as Map<*, *>
                            QuestionModel(
                                type = q["type"] as? String ?: "",
                                question = q["question"] as? String ?: "",
                                correctAnswer = q["correct_answer"] as? String ?: "",
                                points = (q["points"] as? Number)?.toInt() ?: 0,
                                options = (q["options"] as? List<*>)?.map { it.toString() }
                            )
                        }
                        ResponseEntity.ok(QuizData(questions = questions))
                    } else {
                        ResponseEntity.ok(mapOf("error" to "Failed to parse quiz data: no questions found"))
                    }
                } catch (e: Exception) {
                    logger.error("Error parsing quiz data for document {}: {}", documentId, e.message, e)
                    ResponseEntity.ok(mapOf("error" to "Failed to parse quiz data: ${e.message}"))
                }
            } else {
                ResponseEntity.ok(mapOf("error" to "No quiz data available for this document"))
            }
        } catch (e: Exception) {
            logger.error("Error retrieving quiz for document {}: {}", documentId, e.message, e)
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
    
    @GetMapping("/{documentId}/flashcards")
    fun getDocumentFlashcards(@PathVariable documentId: String): ResponseEntity<Any> {
        logger.info("GET /api/documents/{}/flashcards", documentId)
        return try {
            val documentEntity = documentService.getDocumentEntity(documentId)
                ?: return ResponseEntity.notFound().build()
            
            if (documentEntity.flashcardData != null) {
                // Convert JsonNode back to FlashcardResponse using Map approach
                val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                try {
                    // Log the raw JSON for debugging
                    val rawJson = documentEntity.flashcardData.toString()
                    logger.info("Raw flashcard JSON for document {}: {}", documentId, rawJson)
                    
                    // Always parse as Map to avoid Jackson/Kotlin constructor issues
                    val flashcardDataMap = objectMapper.readValue(rawJson, Map::class.java) as Map<*, *>
                    logger.info("Parsed flashcard data map: {}", flashcardDataMap)
                    
                    val responseMap = flashcardDataMap["response"] as? Map<*, *>
                    logger.info("Response map: {}", responseMap)
                    
                    val flashcardsList = responseMap?.get("flashcards") as? List<*>
                    logger.info("Flashcards list: {}", flashcardsList)
                    
                    if (flashcardsList != null) {
                        val flashcards = flashcardsList.map { flashcardMap ->
                            val fc = flashcardMap as Map<*, *>
                            logger.info("Processing flashcard: {}", fc)
                            FlashcardModel(
                                question = fc["question"] as? String ?: "",
                                answer = fc["answer"] as? String ?: "",
                                difficulty = fc["difficulty"] as? String ?: "medium"
                            )
                        }
                        logger.info("Successfully parsed {} flashcards", flashcards.size)
                        ResponseEntity.ok(FlashcardResponse(FlashcardsData(flashcards = flashcards)))
                    } else {
                        logger.warn("No flashcards list found in response map")
                        ResponseEntity.ok(mapOf("error" to "Failed to parse flashcard data: no flashcards found"))
                    }
                } catch (e: Exception) {
                    logger.error("Error parsing flashcard data for document {}: {}", documentId, e.message, e)
                    ResponseEntity.ok(mapOf("error" to "Failed to parse flashcard data: ${e.message}"))
                }
            } else {
                logger.info("No flashcard data available for document: {}", documentId)
                ResponseEntity.ok(mapOf("error" to "No flashcard data available for this document"))
            }
        } catch (e: Exception) {
            logger.error("Error retrieving flashcards for document {}: {}", documentId, e.message, e)
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping
    fun listDocuments(): ResponseEntity<DocumentListResponse> {
        return ResponseEntity.ok(documentService.listDocuments())
    }

    @GetMapping("/{documentId}/download")
    fun downloadFile(@PathVariable documentId: String): ResponseEntity<Any> {
        return try {
            val documentEntity = documentService.getDocumentEntity(documentId)
                ?: return ResponseEntity.notFound().build()
            
            val fileContent = documentService.getFileContent(documentId)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${documentEntity.originalName}\"")
                .contentType(MediaType.parseMediaType(documentEntity.fileType ?: "application/octet-stream"))
                .contentLength(fileContent.size.toLong())
                .body(fileContent)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
    
    @PutMapping("/{documentId}/content")
    fun updateDocumentContent(
        @PathVariable documentId: String,
        @RequestBody request: UpdateDocumentContentRequest
    ): ResponseEntity<Any> {
        return try {
            documentService.updateDocumentContent(documentId, request.summary, request.processedContent)
            ResponseEntity.ok(mapOf("message" to "Document content updated successfully"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{documentId}")
    fun deleteDocument(@PathVariable documentId: String): ResponseEntity<Any> {
        return try {
            documentService.deleteDocument(documentId)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
} 