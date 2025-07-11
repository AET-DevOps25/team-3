package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.entity.DocumentEntity
import de.tum.cit.aet.server.repository.DocumentRepository
import org.slf4j.LoggerFactory
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
import com.fasterxml.jackson.databind.JsonNode

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
    
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)
    
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
        logger.info("Starting async processing for document: {} (ID: {})", event.fileName, event.documentId)
        try {
            // Verify document exists before processing
            if (documentRepository.existsById(event.documentId)) {
                processDocumentAsync(event.documentId, event.fileName, event.fileContent)
            } else {
                logger.error("Document {} not found after transaction commit!", event.documentId)
            }
        } catch (e: Exception) {
            logger.error("Error starting async processing for document {}: {}", event.fileName, e.message, e)
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
    fun updateDocumentContent(documentId: String, summary: String?, processedContent: JsonNode?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        if (summary != null) {
            documentEntity.summary = summary
            // Ensure summaryStatus is set to READY if summary is updated
            documentEntity.summaryStatus = DocumentStatus.READY
        }
        if (processedContent != null) {
            documentEntity.processedContent = processedContent
        }
        documentEntity.updatedAt = LocalDateTime.now()
        documentRepository.save(documentEntity)
    }
    

    @Transactional
    fun updateDocumentQuizData(documentId: String, quizData: QuizResponse?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        // Convert QuizResponse to JsonNode for storage
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        val quizJson = quizData?.let { response: QuizResponse -> 
            objectMapper.readTree(objectMapper.writeValueAsString(response))
        }
        
        if (quizJson != null) {
            documentEntity.quizData = quizJson
            // Ensure quizStatus is set to READY if quiz is updated
            documentEntity.quizStatus = DocumentStatus.READY
        }
        documentEntity.updatedAt = LocalDateTime.now()
        
        documentRepository.save(documentEntity)
    }
    
    @Transactional
    fun updateDocumentFlashcardData(documentId: String, flashcardData: FlashcardResponse?) {
        val documentEntity = documentRepository.findById(documentId)
            .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
        
        // Convert FlashcardResponse to JsonNode for storage
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        val flashcardJson = flashcardData?.let { response: FlashcardResponse -> 
            val jsonString = objectMapper.writeValueAsString(response)
            logger.info("Saving flashcard JSON for document {}: {}", documentId, jsonString)
            objectMapper.readTree(jsonString)
        }
        
        if (flashcardJson != null) {
            documentEntity.flashcardData = flashcardJson
            // Ensure flashcardStatus is set to READY if flashcards are updated
            documentEntity.flashcardStatus = DocumentStatus.READY
            logger.info("Successfully saved flashcard data for document {}", documentId)
        } else {
            logger.warn("No flashcard data to save for document {}", documentId)
        }
        documentEntity.updatedAt = LocalDateTime.now()
        
        documentRepository.save(documentEntity)
    }

    @Transactional
    fun updateQuizStatus(documentId: String, status: DocumentStatus) {
        try {
            val documentEntity = documentRepository.findById(documentId)
                .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
            
            documentEntity.quizStatus = status
            documentEntity.updatedAt = LocalDateTime.now()
            documentRepository.save(documentEntity)
            logger.info("Updated document {} quiz status to {}", documentId, status)
        } catch (e: Exception) {
            logger.error("Error updating document {} quiz status: {}", documentId, e.message, e)
            throw e
        }
    }

    @Transactional
    fun updateSummaryStatus(documentId: String, status: DocumentStatus) {
        try {
            val documentEntity = documentRepository.findById(documentId)
                .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
            
            documentEntity.summaryStatus = status
            documentEntity.updatedAt = LocalDateTime.now()
            documentRepository.save(documentEntity)
            logger.info("Updated document {} summary status to {}", documentId, status)
        } catch (e: Exception) {
            logger.error("Error updating document {} summary status: {}", documentId, e.message, e)
            throw e
        }
    }
    

    @Transactional
    fun updateFlashcardStatus(documentId: String, status: DocumentStatus) {
        try {
            val documentEntity = documentRepository.findById(documentId)
                .orElseThrow { RuntimeException("Document not found with ID: $documentId") }
            
            documentEntity.flashcardStatus = status
            documentEntity.updatedAt = LocalDateTime.now()
            documentRepository.save(documentEntity)
            logger.info("Updated document {} flashcard status to {}", documentId, status)
        } catch (e: Exception) {
            logger.error("Error updating document {} flashcard status: {}", documentId, e.message, e)
            throw e
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
            logger.info("Updated document {} status to {}", documentId, status)
        } catch (e: Exception) {
            logger.error("Error updating document {} status: {}", documentId, e.message, e)
            throw e
        }
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
                logger.info("Processing document async: {} (ID: {})", fileName, documentId)
                
                // Check if document has already been fully processed to avoid re-processing
                val existingDocument = documentRepository.findById(documentId).orElse(null)
                if (existingDocument != null && existingDocument.status == DocumentStatus.READY) {
                    // Check if all content generation is already complete
                    val summaryReady = existingDocument.summaryStatus == DocumentStatus.READY
                    val quizReady = existingDocument.quizStatus == DocumentStatus.READY  
                    val flashcardsReady = existingDocument.flashcardStatus == DocumentStatus.READY
                    
                    if (summaryReady && quizReady && flashcardsReady) {
                        logger.info("Document {} already fully processed, skipping re-processing", fileName)
                        return@runAsync
                    }
                    
                    // Only process the missing parts
                    logger.info("Document {} partially processed, completing missing parts: summary={}, quiz={}, flashcards={}", 
                              fileName, summaryReady, quizReady, flashcardsReady)
                    
                    // Generate missing summary
                    if (!summaryReady && existingDocument.summaryStatus != DocumentStatus.PROCESSING) {
                        logger.info("Generating missing summary for document: {}", fileName)
                        updateSummaryStatus(documentId, DocumentStatus.PROCESSING)
                        val summary = genAiService.generateSummary(documentId)
                        if (summary != null) {
                            logger.info("Summary generated successfully")
                            updateDocumentContent(documentId, summary, null)
                        } else {
                            logger.error("Failed to generate summary for document: {}", fileName)
                            updateSummaryStatus(documentId, DocumentStatus.ERROR)
                        }
                    }
                    
                    // Generate missing quiz
                    if (!quizReady && existingDocument.quizStatus != DocumentStatus.PROCESSING) {
                        logger.info("Generating missing quiz for document: {}", fileName)
                        updateQuizStatus(documentId, DocumentStatus.PROCESSING)
                        val quizQuestions = genAiService.generateQuiz(documentId)
                        if (quizQuestions != null) {
                            logger.info("Quiz generated successfully")
                            val quizResponse = QuizResponse(QuizData(questions = quizQuestions))
                            updateDocumentQuizData(documentId, quizResponse)
                        } else {
                            logger.error("Failed to generate quiz for document: {}", fileName)
                            updateQuizStatus(documentId, DocumentStatus.ERROR)
                        }
                    }
                    
                    // Generate missing flashcards  
                    if (!flashcardsReady && existingDocument.flashcardStatus != DocumentStatus.PROCESSING) {
                        logger.info("Generating missing flashcards for document: {}", fileName)
                        updateFlashcardStatus(documentId, DocumentStatus.PROCESSING)
                        val flashcardModels = genAiService.generateFlashcards(documentId)
                        if (flashcardModels != null) {
                            logger.info("Flashcards generated successfully")
                            val flashcardResponse = FlashcardResponse(FlashcardsData(flashcards = flashcardModels))
                            updateDocumentFlashcardData(documentId, flashcardResponse)
                        } else {
                            logger.error("Failed to generate flashcards for document: {}", fileName)
                            updateFlashcardStatus(documentId, DocumentStatus.ERROR)
                        }
                    }
                    
                    return@runAsync
                }
                
                // Continue with normal processing for new documents
                // Update overall status to PROCESSING
                updateDocumentStatus(documentId, DocumentStatus.PROCESSING)
                
                // Check if GenAI service is available
                if (!genAiService.isServiceAvailable()) {
                    logger.warn("GenAI service is not available, skipping processing for document: {}", fileName)
                    updateDocumentStatus(documentId, DocumentStatus.ERROR)
                    return@runAsync
                }
                
                // Create session in GenAI service
                logger.info("Creating GenAI session for document: {}", fileName)
                val sessionResult = genAiService.createSession(documentId, fileName, fileContent)
                
                if (sessionResult != null) {
                    logger.info("GenAI session created successfully")
                    updateDocumentStatus(documentId, DocumentStatus.READY)
                    
                    // Generate summary
                    logger.info("Generating summary for document: {}", fileName)
                    updateSummaryStatus(documentId, DocumentStatus.PROCESSING)
                    val summary = genAiService.generateSummary(documentId)

                    if (summary != null) {
                        logger.info("Summary generated successfully")
                        updateDocumentContent(documentId, summary, null)
                        // Remove redundant call - updateDocumentContent already sets status to READY
                    } else {
                        logger.error("Failed to generate summary for document: {}", fileName)
                        updateSummaryStatus(documentId, DocumentStatus.ERROR)
                    }
                    
                    // Generate quiz
                    logger.info("Generating quiz for document: {}", fileName)
                    updateQuizStatus(documentId, DocumentStatus.PROCESSING)
                    val quizQuestions = genAiService.generateQuiz(documentId)

                    if (quizQuestions != null) {
                        logger.info("Quiz generated successfully")
                        val quizResponse = QuizResponse(QuizData(questions = quizQuestions))
                        updateDocumentQuizData(documentId, quizResponse)
                        // Remove redundant call - updateDocumentQuizData already sets status to READY
                    } else {
                        logger.error("Failed to generate quiz for document: {}", fileName)
                        updateQuizStatus(documentId, DocumentStatus.ERROR)
                    }
                    
                    // Generate flashcards
                    logger.info("Generating flashcards for document: {}", fileName)
                    updateFlashcardStatus(documentId, DocumentStatus.PROCESSING)
                    val flashcardModels = genAiService.generateFlashcards(documentId)

                    if (flashcardModels != null) {
                        logger.info("Flashcards generated successfully")
                        val flashcardResponse = FlashcardResponse(FlashcardsData(flashcards = flashcardModels))
                        updateDocumentFlashcardData(documentId, flashcardResponse)
                        // Remove redundant call - updateDocumentFlashcardData already sets status to READY
                    } else {
                        logger.error("Failed to generate flashcards for document: {}", fileName)
                        updateFlashcardStatus(documentId, DocumentStatus.ERROR)
                    }
                } else {
                    logger.error("Failed to create GenAI session")
                    updateDocumentStatus(documentId, DocumentStatus.ERROR)
                }
                
            } catch (e: Exception) {
                logger.error("Error processing document {}: {}", fileName, e.message, e)
                updateDocumentStatus(documentId, DocumentStatus.ERROR)
            }
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