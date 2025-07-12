package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.entity.UserEntity
import de.tum.cit.aet.server.service.DocumentService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["*"])
class DocumentController(
    private val documentService: DocumentService
) {
    
    private val logger = LoggerFactory.getLogger(DocumentController::class.java)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadDocuments(
        @RequestParam("files") files: List<MultipartFile>,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<DocumentUploadResponse> {
        logger.info("POST /api/documents/upload - {} files", files.size)
        return try {
            val response = documentService.uploadDocuments(files, user)
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
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<DocumentStatusResponse> {
        logger.info("GET /api/documents/{}/status", documentId)
        return try {
            val response = documentService.getDocumentStatus(documentId, user)
            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            logger.error("Error getting document status: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{documentId}/content")
    fun getDocumentContent(
        @PathVariable documentId: String,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<DocumentContentResponse> {
        logger.info("GET /api/documents/{}/content", documentId)
        return try {
            val response = documentService.getDocumentContent(documentId, user)
            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            logger.error("Error getting document content: {}", e.message)
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun listDocuments(
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<DocumentListResponse> {
        logger.info("GET /api/documents")
        return try {
            val response = documentService.listDocuments(user)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error listing documents: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{documentId}/download")
    fun downloadDocument(
        @PathVariable documentId: String,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<ByteArray> {
        logger.info("GET /api/documents/{}/download", documentId)
        return try {
            val documentEntity = documentService.getDocumentEntity(documentId, user)
            if (documentEntity != null) {
                val fileContent = documentService.getFileContent(documentId, user)
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
}