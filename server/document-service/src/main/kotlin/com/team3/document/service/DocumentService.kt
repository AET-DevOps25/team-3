package com.team3.document.service

import com.team3.document.entity.Document
import com.team3.document.repository.DocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import jakarta.persistence.EntityNotFoundException

@Service
class DocumentService(private val documentRepository: DocumentRepository) {

    fun getAllDocuments(): List<Document> = documentRepository.findAll()

    fun getDocumentById(id: UUID): Document = documentRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Document not found with id: $id") }

    @Transactional
    fun createDocument(document: Document): Document {
        document.updatedAt = LocalDateTime.now()
        return documentRepository.save(document)
    }

    @Transactional
    fun updateDocument(id: UUID, updatedDocument: Document): Document {
        val existingDocument = getDocumentById(id)
        
        existingDocument.apply {
            title = updatedDocument.title
            content = updatedDocument.content
            metadata = updatedDocument.metadata
            updatedAt = LocalDateTime.now()
        }
        
        return documentRepository.save(existingDocument)
    }

    @Transactional
    fun deleteDocument(id: UUID) {
        if (!documentRepository.existsById(id)) {
            throw EntityNotFoundException("Document not found with id: $id")
        }
        documentRepository.deleteById(id)
    }
} 