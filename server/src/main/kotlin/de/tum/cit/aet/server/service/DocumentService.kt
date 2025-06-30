package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.entity.DocumentEntity
import de.tum.cit.aet.server.repository.DocumentRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture

// Event class for document upload completion
data class DocumentUploadedEvent(
    val documentId: String,
    val fileName: String,
    val fileContent: ByteArray
)

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val genAiService: GenAiService,
    private val eventPublisher: ApplicationEventPublisher
) {
    
    @Transactional
    fun uploadFiles(files: Array<MultipartFile>): DocumentUploadResponse {
        val documentIds = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        for (file in files) {
            try {
                // Validate file
                if (file.isEmpty) {
                    errors.add("File ${file.originalFilename} is empty")
                    continue
                }
                
                // Generate unique ID
                val documentId = UUID.randomUUID().toString()
                
                // Convert file to Base64
                val base64Content = Base64.getEncoder().encodeToString(file.bytes)
                
                // Create and save document entity
                val documentEntity = DocumentEntity(
                    id = documentId,
                    originalName = file.originalFilename ?: "unknown",
                    fileSize = file.size,
                    fileType = extractFileType(file.originalFilename ?: ""),
                    fileContent = base64Content,
                    uploadDate = LocalDateTime.now(),
                    status = DocumentStatus.UPLOADED
                )
                
                documentRepository.save(documentEntity)
                documentIds.add(documentId)
                
                // Publish event for async processing AFTER transaction commits
                eventPublisher.publishEvent(
                    DocumentUploadedEvent(
                        documentId = documentId,
                        fileName = file.originalFilename ?: "unknown",
                        fileContent = file.bytes
                    )
                )
                
            } catch (e: Exception) {
                errors.add("Error uploading file ${file.originalFilename}: ${e.message}")
            }
        }
        
        // If all files failed, throw exception
        if (documentIds.isEmpty() && errors.isNotEmpty()) {
            throw RuntimeException("All file uploads failed: ${errors.joinToString(", ")}")
        }
        
        return DocumentUploadResponse(
            documentIds = documentIds,
            status = if (errors.isEmpty()) "success" else "partial_success"
        )
    }
    
    // This handler runs AFTER the transaction commits
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleDocumentUploaded(event: DocumentUploadedEvent) {
        println("Starting async processing for document: ${event.fileName} (ID: ${event.documentId})")
        try {
            // Verify document exists before processing
            if (documentRepository.existsById(event.documentId)) {
                processDocumentAsync(event.documentId, event.fileName, event.fileContent)
            } else {
                println("Document ${event.documentId} not found after transaction commit!")
            }
        } catch (e: Exception) {
            println("Error starting async processing for document ${event.fileName}: ${e.message}")
        }
    }
    
    fun getDocumentStatus(documentId: String): DocumentStatusResponse {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        return DocumentStatusResponse(
            documentId = documentEntity.id,
            status = documentEntity.status,
            documentName = documentEntity.originalName,
            uploadDate = documentEntity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
            summary = documentEntity.summary
        )
    }
    
    fun getDocumentEntity(documentId: String): DocumentEntity? {
        return documentRepository.findById(documentId).orElse(null)
    }
    
    fun getFileContent(documentId: String): ByteArray {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        return Base64.getDecoder().decode(documentEntity.fileContent)
    }
    
    fun getDocumentContent(documentId: String): DocumentContentResponse {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        return DocumentContentResponse(
            id = documentEntity.id,
            originalName = documentEntity.originalName,
            summary = documentEntity.summary,
            processedContent = documentEntity.processedContent,
            status = documentEntity.status,
            uploadDate = documentEntity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
            updatedAt = documentEntity.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
        )
    }
    
    fun listDocuments(): DocumentListResponse {
        val documents = documentRepository.findAll().map { entity ->
            DocumentInfo(
                id = entity.id,
                name = entity.originalName,
                size = entity.fileSize,
                uploadDate = entity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
                type = entity.fileType ?: "unknown",
                status = entity.status,
                summary = entity.summary
            )
        }
        
        return DocumentListResponse(documents = documents)
    }
    
    @Transactional
    fun updateDocumentContent(documentId: String, summary: String?, processedContent: com.fasterxml.jackson.databind.JsonNode?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.summary = summary
        documentEntity.processedContent = processedContent
        documentEntity.status = DocumentStatus.PROCESSED
        documentEntity.updatedAt = LocalDateTime.now()
        
        documentRepository.save(documentEntity)
    }
    
    @Transactional
    fun deleteDocument(documentId: String) {
        if (!documentRepository.existsById(documentId)) {
            throw RuntimeException("Document not found with ID: $documentId")
        }
        documentRepository.deleteById(documentId)
    }
    
    @Async
    fun processDocumentAsync(documentId: String, fileName: String, fileContent: ByteArray): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                println("Processing document async: $fileName (ID: $documentId)")
                
                // Update status to PROCESSING
                updateDocumentStatus(documentId, DocumentStatus.PROCESSING)
                
                // Create session in GenAI service
                println("Creating GenAI session for document: $fileName")
                val sessionResult = genAiService.createSession(documentId, fileName, fileContent)
                
                if (sessionResult != null) {
                    println("GenAI session created successfully")
                    
                    // Generate summary
                    println("Generating summary for document: $fileName")
                    val summary = genAiService.generateSummary(documentId)
                    
                    if (summary != null) {
                        println("Summary generated successfully")
                        // Update document with summary and mark as processed
                        updateDocumentContent(documentId, summary, null)
                        updateDocumentStatus(documentId, DocumentStatus.READY)
                    } else {
                        println("Failed to generate summary")
                        updateDocumentStatus(documentId, DocumentStatus.ERROR)
                    }
                } else {
                    println("Failed to create GenAI session")
                    updateDocumentStatus(documentId, DocumentStatus.ERROR)
                }
                
            } catch (e: Exception) {
                println("Error processing document $fileName: ${e.message}")
                e.printStackTrace()
                updateDocumentStatus(documentId, DocumentStatus.ERROR)
            }
        }
    }
    
    @Transactional
    fun updateDocumentStatus(documentId: String, status: DocumentStatus) {
        try {
            val documentEntity = documentRepository.findById(documentId)
                .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
            
            documentEntity.status = status
            documentEntity.updatedAt = LocalDateTime.now()
            documentRepository.save(documentEntity)
            println("Updated document $documentId status to $status")
        } catch (e: Exception) {
            println("Error updating document $documentId status: ${e.message}")
            throw e
        }
    }
    
    private fun extractFileType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }
} 