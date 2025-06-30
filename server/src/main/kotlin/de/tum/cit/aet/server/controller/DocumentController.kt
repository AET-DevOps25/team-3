package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.service.DocumentService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = ["*"])
class DocumentController(private val documentService: DocumentService) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFiles(@RequestParam("files") files: Array<MultipartFile>): ResponseEntity<Any> {
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
        return try {
            val response = documentService.getDocumentStatus(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/{documentId}/content")
    fun getDocumentContent(@PathVariable documentId: String): ResponseEntity<Any> {
        return try {
            val response = documentService.getDocumentContent(documentId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
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