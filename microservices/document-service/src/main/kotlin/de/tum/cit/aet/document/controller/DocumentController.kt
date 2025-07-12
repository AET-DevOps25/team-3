package de.tum.cit.aet.document.controller

import de.tum.cit.aet.document.dto.*
import de.tum.cit.aet.document.service.DocumentService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["*"])
class DocumentController(
    private val documentService: DocumentService
) {
    
    private val logger = LoggerFactory.getLogger(DocumentController::class.java)

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun uploadDocuments(
        @RequestParam("files") files: List<MultipartFile>,
        authentication: Authentication
    ): ResponseEntity<DocumentUploadResponse> {
        logger.info("POST /api/documents/upload - {} files", files.size)
        return try {
            val response = documentService.uploadDocuments(files, authentication.name)
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
        authentication: Authentication
    ): ResponseEntity<DocumentStatusResponse> {
        logger.info("GET /api/documents/{}/status", documentId)
        return try {
            val response = documentService.getDocumentStatus(documentId, authentication.name)
            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            logger.error("Error getting document status: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{documentId}/content")
    fun getDocumentContent(
        @PathVariable documentId: String,
        authentication: Authentication
    ): ResponseEntity<DocumentContentResponse> {
        logger.info("GET /api/documents/{}/content", documentId)
        return try {
            val response = documentService.getDocumentContent(documentId, authentication.name)
            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            logger.error("Error getting document content: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun listDocuments(
        authentication: Authentication
    ): ResponseEntity<DocumentListResponse> {
        logger.info("GET /api/documents")
        return try {
            val response = documentService.listDocuments(authentication.name)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error listing documents: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
    
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "healthy", "service" to "document-service"))
    }
} 