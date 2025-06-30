package com.team3.document.controller

import com.team3.document.entity.Document
import com.team3.document.service.DocumentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

data class CreateDocumentRequest(
    val title: String,
    val description: String,
    val content: JsonNode,
    val metadata: JsonNode
)

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    fun getAllDocuments(): ResponseEntity<List<Document>> =
        ResponseEntity.ok(documentService.getAllDocuments())

    @GetMapping("/{id}")
    fun getDocumentById(@PathVariable id: UUID): ResponseEntity<Document> =
        ResponseEntity.ok(documentService.getDocumentById(id))

    @PostMapping
    fun createDocument(@RequestBody request: CreateDocumentRequest): ResponseEntity<Document> {
        // Debug log for content and metadata
        println("[DEBUG] Received content type: ${request.content.javaClass.name}, value: ${request.content}")
        println("[DEBUG] Received metadata type: ${request.metadata.javaClass.name}, value: ${request.metadata}")
        
        // Create Document entity with JsonNode fields directly
        val document = Document(
            title = request.title,
            description = request.description.ifEmpty { "No description provided" },
            content = request.content,
            metadata = request.metadata
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(document))
    }

    @PutMapping("/{id}")
    fun updateDocument(
        @PathVariable id: UUID,
        @RequestBody updatedDocument: Document
    ): ResponseEntity<Document> =
        ResponseEntity.ok(documentService.updateDocument(id, updatedDocument))

    @DeleteMapping("/{id}")
    fun deleteDocument(@PathVariable id: UUID): ResponseEntity<Unit> {
        documentService.deleteDocument(id)
        return ResponseEntity.noContent().build()
    }
} 