package com.team3.document.controller

import com.team3.document.entity.Document
import com.team3.document.service.DocumentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/documents")
class DocumentController(private val documentService: DocumentService) {

    @GetMapping
    fun getAllDocuments(): ResponseEntity<List<Document>> =
        ResponseEntity.ok(documentService.getAllDocuments())

    @GetMapping("/{id}")
    fun getDocumentById(@PathVariable id: UUID): ResponseEntity<Document> =
        ResponseEntity.ok(documentService.getDocumentById(id))

    @PostMapping
    fun createDocument(@RequestBody document: Document): ResponseEntity<Document> =
        ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(document))

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