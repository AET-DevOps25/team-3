package de.tum.cit.aet.document.controller

import de.tum.cit.aet.document.dto.*
import de.tum.cit.aet.document.service.DocumentService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["*"])
class DocumentController(
    private val documentService: DocumentService
) {
    
    private val logger = LoggerFactory.getLogger(DocumentController::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        logger.error("Validation error: {}", errors)
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                error = "Validation failed",
                message = "Request validation failed: $errors",
                timestamp = LocalDateTime.now().format(dateFormatter)
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.error("Message not readable: {}", ex.message)
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                error = "Invalid request",
                message = "Could not read request body: ${ex.message}",
                timestamp = LocalDateTime.now().format(dateFormatter)
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                error = "Internal server error",
                message = "An unexpected error occurred: ${ex.message}",
                timestamp = LocalDateTime.now().format(dateFormatter)
            )
        )
    }

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadDocuments(
        @RequestParam("files") files: List<MultipartFile>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<DocumentUploadResponse> {
        val userId = userDetails.username
        logger.info("POST /documents/upload - {} files for user: {}", files.size, userId)
        return try {
            val response = documentService.uploadDocuments(files, userId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error uploading documents: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DocumentUploadResponse(
                    documentIds = emptyList(),
                    status = "error",
                    message = e.message
                ))
        }
    }

    @GetMapping("/{documentId}/status")
    fun getDocumentStatus(
        @PathVariable documentId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<DocumentStatusResponse> {
        val userId = userDetails.username
        logger.info("GET /documents/{}/status for user: {}", documentId, userId)
        return try {
            val response = documentService.getDocumentStatus(documentId, userId)
            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            logger.error("Error getting document status: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{documentId}/content")
    fun getDocumentContent(
        @PathVariable documentId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<DocumentContentResponse> {
        val userId = userDetails.username
        logger.info("GET /documents/{}/content for user: {}", documentId, userId)
        return try {
            val response = documentService.getDocumentContent(documentId, userId)
            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            logger.error("Error getting document content: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun listDocuments(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<DocumentListResponse> {
        val userId = userDetails.username
        logger.info("GET /documents for user: {}", userId)
        return try {
            val response = documentService.listDocuments(userId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error listing documents: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{documentId}/download")
    fun downloadDocument(
        @PathVariable documentId: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        val userId = userDetails.username
        logger.info("GET /documents/{}/download for user: {}", documentId, userId)
        return try {
            val documentEntity = documentService.getDocumentEntity(documentId, userId)
            if (documentEntity != null) {
                val fileContent = documentService.getFileContent(documentId, userId)
                val headers = HttpHeaders()
                headers.contentType = MediaType.APPLICATION_OCTET_STREAM
                headers.setContentDispositionFormData("attachment", documentEntity.originalName)
                ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: RuntimeException) {
            logger.error("Error downloading document: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }
    
    // Internal endpoint for service-to-service communication
    @GetMapping("/internal/{documentId}/content")
    fun getDocumentContentInternal(
        @PathVariable documentId: String
    ): ResponseEntity<DocumentContentResponse> {
        logger.info("INTERNAL ENDPOINT HIT: /internal/{}/content", documentId)
        logger.info("GET /internal/documents/{}/content", documentId)
        return try {
            // Get the document entity first to extract the user ID
            val documentEntity = documentService.getDocumentEntityById(documentId)
            if (documentEntity != null) {
                val response = documentService.getDocumentContent(documentId, documentEntity.userId)
                ResponseEntity.ok(response)
            } else {
                logger.error("Document not found: {}", documentId)
                ResponseEntity.notFound().build()
            }
        } catch (e: RuntimeException) {
            logger.error("Error getting document content: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }
    
    // Internal endpoint to fetch raw Base64 file content (service-to-service)
    @GetMapping("/internal/{documentId}/file")
    fun getDocumentFileInternal(@PathVariable documentId: String): ResponseEntity<Map<String, String>> {
        logger.info("INTERNAL ENDPOINT HIT: /internal/{}/file", documentId)
        return try {
            val document = documentService.getDocumentEntityById(documentId)
            if (document != null) {
                ResponseEntity.ok(mapOf("fileContent" to (document.fileContent ?: ""),
                                          "fileName" to document.originalName))
            } else {
                logger.error("Document not found: {}", documentId)
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error getting document file: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // Internal endpoint for updating document summary (service-to-service)
    @PutMapping("/internal/{documentId}/summary")
    fun updateDocumentSummaryInternal(
        @PathVariable documentId: String,
        @Valid @RequestBody request: SummaryUpdateRequest
    ): ResponseEntity<String> {
        logger.info("INTERNAL ENDPOINT HIT: PUT /internal/{}/summary", documentId)
        logger.info("Received summary update request for document {}: {}", documentId, request)
        
        return try {
            documentService.updateDocumentSummary(documentId, request.summary)
            logger.info("Summary updated successfully for document {}", documentId)
            ResponseEntity.ok("Summary updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating summary for document {}: {}", documentId, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating summary: ${e.message}")
        }
    }

    // Internal endpoint for updating document quiz (service-to-service)
    @PutMapping("/internal/{documentId}/quiz")
    fun updateDocumentQuizInternal(
        @PathVariable documentId: String,
        @RequestBody quizData: Map<String, Any>
    ): ResponseEntity<String> {
        logger.info("INTERNAL ENDPOINT HIT: PUT /internal/{}/quiz", documentId)
        logger.info("Received quiz update request for document {}: {}", documentId, quizData)
        
        return try {
            documentService.updateDocumentQuiz(documentId, quizData)
            logger.info("Quiz updated successfully for document {}", documentId)
            ResponseEntity.ok("Quiz updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating quiz for document {}: {}", documentId, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating quiz: ${e.message}")
        }
    }

    // Internal endpoint for updating document flashcards (service-to-service)
    @PutMapping("/internal/{documentId}/flashcards")
    fun updateDocumentFlashcardsInternal(
        @PathVariable documentId: String,
        @RequestBody flashcardData: List<Map<String, Any>>
    ): ResponseEntity<String> {
        logger.info("INTERNAL ENDPOINT HIT: PUT /internal/{}/flashcards", documentId)
        logger.info("Received flashcard update request for document {}: {}", documentId, flashcardData)
        
        return try {
            documentService.updateDocumentFlashcards(documentId, flashcardData)
            logger.info("Flashcards updated successfully for document {}", documentId)
            ResponseEntity.ok("Flashcards updated successfully")
        } catch (e: Exception) {
            logger.error("Error updating flashcards for document {}: {}", documentId, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating flashcards: ${e.message}")
        }
    }
} 