package de.tum.cit.aet.document.service

import de.tum.cit.aet.document.dto.*
import de.tum.cit.aet.document.entity.DocumentEntity
import de.tum.cit.aet.document.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class DocumentService(
    private val documentRepository: DocumentRepository
) {
    
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)
    private val objectMapper = ObjectMapper()
    
    @Transactional
    fun uploadDocuments(files: List<MultipartFile>, userId: String): DocumentUploadResponse {
        val documentIds = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        files.forEach { file ->
            try {
                val documentId = UUID.randomUUID().toString()
                val base64Content = Base64.getEncoder().encodeToString(file.bytes)
                
                val documentEntity = DocumentEntity(
                    id = documentId,
                    originalName = file.originalFilename ?: "unknown",
                    fileSize = file.size,
                    fileType = extractFileType(file.originalFilename ?: ""),
                    fileContent = base64Content,
                    uploadDate = LocalDateTime.now(),
                    status = DocumentStatus.UPLOADED,
                    userId = userId
                )
                
                documentRepository.save(documentEntity)
                documentIds.add(documentId)
                
                logger.info("Document uploaded: {} (ID: {})", file.originalFilename, documentId)
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
            status = if (errors.isEmpty()) "success" else "partial_success",
            message = if (errors.isNotEmpty()) "Some files failed to upload: ${errors.joinToString(", ")}" else null
        )
    }
    
    @Transactional(readOnly = true)
    fun getDocumentStatus(documentId: String, userId: String): DocumentStatusResponse {
        val documentEntity = documentRepository.findByIdAndUserId(documentId, userId)
            ?: throw RuntimeException("Document not found with ID: $documentId")
        
        return DocumentStatusResponse(
            documentId = documentEntity.id,
            status = documentEntity.status,
            documentName = documentEntity.originalName,
            uploadDate = documentEntity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
            summary = documentEntity.summary
        )
    }
    
    @Transactional(readOnly = true)
    fun listDocuments(userId: String): DocumentListResponse {
        val documents = documentRepository.findByUserId(userId).map { entity ->
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
    
    @Transactional(readOnly = true)
    fun getDocumentContent(documentId: String, userId: String): DocumentContentResponse {
        val documentEntity = documentRepository.findByIdAndUserId(documentId, userId)
            ?: throw RuntimeException("Document not found with ID: $documentId")
        
        return DocumentContentResponse(
            id = documentEntity.id,
            originalName = documentEntity.originalName,
            summary = documentEntity.summary,
            processedContent = documentEntity.processedContent,
            quizData = documentEntity.quizData,
            flashcardData = documentEntity.flashcardData,
            status = documentEntity.status,
            summaryStatus = documentEntity.summaryStatus,
            quizStatus = documentEntity.quizStatus,
            flashcardStatus = documentEntity.flashcardStatus,
            uploadDate = documentEntity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
            updatedAt = documentEntity.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
        )
    }
    
    @Transactional
    fun updateDocumentContent(documentId: String, summary: String?, processedContent: JsonNode?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.summary = summary
        documentEntity.processedContent = processedContent
        documentEntity.updatedAt = LocalDateTime.now()
        
        documentRepository.save(documentEntity)
        logger.info("Document content updated for document: {}", documentId)
    }
    
    @Transactional
    fun updateDocumentStatus(documentId: String, status: DocumentStatus) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.status = status
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
        logger.info("Updated document {} status to {}", documentId, status)
    }
    
    @Transactional
    fun updateSummaryStatus(documentId: String, status: DocumentStatus) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.summaryStatus = status
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
        logger.info("Updated document {} summary status to {}", documentId, status)
    }
    
    @Transactional
    fun updateQuizStatus(documentId: String, status: DocumentStatus) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.quizStatus = status
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
        logger.info("Updated document {} quiz status to {}", documentId, status)
    }
    
    @Transactional
    fun updateFlashcardStatus(documentId: String, status: DocumentStatus) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.flashcardStatus = status
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
        logger.info("Updated document {} flashcard status to {}", documentId, status)
    }
    
    @Transactional
    fun updateDocumentQuizData(documentId: String, quizData: JsonNode?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.quizData = quizData
        documentEntity.quizStatus = DocumentStatus.READY
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
        logger.info("Updated quiz data for document: {}", documentId)
    }
    
    @Transactional
    fun updateDocumentFlashcardData(documentId: String, flashcardData: JsonNode?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        documentEntity.flashcardData = flashcardData
        documentEntity.flashcardStatus = DocumentStatus.READY
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
        logger.info("Updated flashcard data for document: {}", documentId)
    }
    
    private fun extractFileType(filename: String): String {
        return if (filename.contains(".")) {
            filename.substringAfterLast(".")
        } else {
            "unknown"
        }
    }
} 