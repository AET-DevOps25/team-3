package de.tum.cit.aet.document.service

import de.tum.cit.aet.document.dto.*
import de.tum.cit.aet.document.entity.DocumentEntity
import de.tum.cit.aet.document.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val webClient: WebClient,
    private val eventPublisher: ApplicationEventPublisher
) {
    
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    @Value("\${genai.service.url:http://genai-service:8085}")
    private lateinit var genaiServiceUrl: String
    
    @Transactional
    fun uploadDocuments(files: List<MultipartFile>, userId: String): DocumentUploadResponse {
        logger.info("Uploading {} documents for user: {}", files.size, userId)
        
        val documentIds = mutableListOf<String>()
        
        for (file in files) {
            try {
                val documentId = UUID.randomUUID().toString()
                val fileContent = Base64.getEncoder().encodeToString(file.bytes)
                
                val document = DocumentEntity(
                    id = documentId,
                    originalName = file.originalFilename ?: "unknown",
                    fileSize = file.size,
                    fileType = file.contentType,
                    fileContent = fileContent,
                    userId = userId
                )
                
                documentRepository.save(document)
                documentIds.add(documentId)
                
                logger.info("Document uploaded successfully: {} for user: {}", documentId, userId)
            } catch (e: Exception) {
                logger.error("Error uploading document: {}", e.message, e)
                throw RuntimeException("Failed to upload document: ${file.originalFilename}")
            }
        }
        
        // Publish event for processing after transaction commits
        eventPublisher.publishEvent(DocumentProcessingEvent(documentIds, userId))
        
        val response = DocumentUploadResponse(
            documentIds = documentIds,
            status = "success",
            message = "Documents uploaded successfully"
        )
        
        return response
    }

    @Transactional
    fun updateDocumentSummary(documentId: String, summary: String) {
        logger.info("Updating summary for document {}", documentId)
        logger.info("Received summary content: {}", summary)
        
        val document = documentRepository.findById(documentId).orElse(null)
        if (document != null) {
            logger.info("Found document {}, current summary: {}", documentId, document.summary)
            document.summary = summary
            document.summaryStatus = DocumentStatus.PROCESSED
            document.status = DocumentStatus.PROCESSED
            val savedDocument = documentRepository.save(document)
            logger.info("Document {} updated. New summary: {}, status: {}, summaryStatus: {}", 
                documentId, savedDocument.summary, savedDocument.status, savedDocument.summaryStatus)
        } else {
            logger.error("Document not found for summary update: {}", documentId)
        }
    }

    @Transactional
    fun updateDocumentQuiz(documentId: String, quizData: Map<String, Any>) {
        logger.info("Updating quiz for document {}", documentId)
        logger.info("Received quiz data: {}", quizData)
        
        val document = documentRepository.findById(documentId).orElse(null)
        if (document != null) {
            logger.info("Found document {}, updating quiz data", documentId)
            // Convert Map to JsonNode
            val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            document.quizData = objectMapper.valueToTree(quizData)
            document.quizStatus = DocumentStatus.PROCESSED
            val savedDocument = documentRepository.save(document)
            logger.info("Document {} quiz updated. Status: {}, quizStatus: {}", 
                documentId, savedDocument.status, savedDocument.quizStatus)
        } else {
            logger.error("Document not found for quiz update: {}", documentId)
        }
    }

    @Transactional
    fun updateDocumentFlashcards(documentId: String, flashcardData: List<Map<String, Any>>) {
        logger.info("Updating flashcards for document {}", documentId)
        logger.info("Received flashcard data: {}", flashcardData)
        
        val document = documentRepository.findById(documentId).orElse(null)
        if (document != null) {
            logger.info("Found document {}, updating flashcard data", documentId)
            // Convert List to JsonNode
            val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
            document.flashcardData = objectMapper.valueToTree(flashcardData)
            document.flashcardStatus = DocumentStatus.PROCESSED
            val savedDocument = documentRepository.save(document)
            logger.info("Document {} flashcards updated. Status: {}, flashcardStatus: {}", 
                documentId, savedDocument.status, savedDocument.flashcardStatus)
        } else {
            logger.error("Document not found for flashcard update: {}", documentId)
        }
    }

    // Event listener that runs after transaction commits
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDocumentProcessingEvent(event: DocumentProcessingEvent) {
        logger.info("Transaction committed, starting processing for {} documents", event.documentIds.size)
        
        // Add a larger delay to ensure the transaction is fully visible to other connections
        Thread.sleep(500)
        
        for (documentId in event.documentIds) {
            logger.info("Starting background processing for document: {} for user: {}", documentId, event.userId)
            Thread {
                try {
                    // Retry up to 3 times to ensure the document is visible in the DB
                    repeat(3) { attempt ->
                        val document = documentRepository.findById(documentId).orElse(null)
                        if (document != null) {
                            logger.info("Document {} found on attempt {}, starting async processing", documentId, attempt + 1)
                            processDocumentAsync(documentId, event.userId)
                            return@Thread
                        }
                        logger.warn("Document {} not found on attempt {}, retrying...", documentId, attempt + 1)
                        Thread.sleep(300)
                    }
                    logger.error("Document {} not found in database after retries, skipping processing", documentId)
                } catch (e: Exception) {
                    logger.error("Error in background processing for document {}: {}", documentId, e.message, e)
                }
            }.start()
        }
    }
    

    

    
    @Async
    fun processDocumentAsync(documentId: String, userId: String) {
        try {
            logger.info("Starting async processing for document: {} for user: {}", documentId, userId)
            
            // Update document status to processing
            val document = documentRepository.findByUserIdAndId(userId, documentId)
            if (document != null) {
                document.status = DocumentStatus.PROCESSING
                document.summaryStatus = DocumentStatus.PROCESSING
                document.quizStatus = DocumentStatus.PROCESSING
                document.flashcardStatus = DocumentStatus.PROCESSING
                documentRepository.save(document)
            }
            
            // Call GenAI service to process the document (generate all content types in parallel)
            val request = mapOf(
                "documentId" to documentId,
                "userId" to userId,
                "processingType" to "ALL"
            )
            
            val response = webClient.post()
                .uri("$genaiServiceUrl/api/genai/process")
                .header("X-User-ID", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()
            
            logger.info("GenAI processing response for document {}: {}", documentId, response)
            
            // Update document status based on response
            if (response != null && response["status"] == "QUEUED") {
                val updatedDocument = documentRepository.findByUserIdAndId(userId, documentId)
                if (updatedDocument != null) {
                    updatedDocument.status = DocumentStatus.PROCESSING
                    documentRepository.save(updatedDocument)
                }
            }
            
        } catch (e: Exception) {
            logger.error("Error processing document {}: {}", documentId, e.message, e)
            
            // Update document status to error
            val document = documentRepository.findByUserIdAndId(userId, documentId)
            if (document != null) {
                document.status = DocumentStatus.ERROR
                document.summaryStatus = DocumentStatus.ERROR
                document.quizStatus = DocumentStatus.ERROR
                document.flashcardStatus = DocumentStatus.ERROR
                documentRepository.save(document)
            }
        }
    }
    
    @Transactional(readOnly = true)
    fun getDocumentStatus(documentId: String, userId: String): DocumentStatusResponse {
        val document = documentRepository.findByUserIdAndId(userId, documentId)
            ?: throw RuntimeException("Document not found")
        
        return DocumentStatusResponse(
            documentId = document.id,
            status = document.status,
            documentName = document.originalName,
            uploadDate = document.uploadDate.format(dateFormatter),
            processingProgress = calculateProgress(document),
            summary = document.summary
        )
    }
    
    @Transactional(readOnly = true)
    fun getDocumentContent(documentId: String, userId: String): DocumentContentResponse {
        val document = documentRepository.findByUserIdAndId(userId, documentId)
            ?: throw RuntimeException("Document not found")
        
        return DocumentContentResponse(
            id = document.id,
            originalName = document.originalName,
            summary = document.summary,
            processedContent = document.processedContent,
            quizData = document.quizData,
            flashcardData = document.flashcardData,
            status = document.status,
            summaryStatus = document.summaryStatus,
            quizStatus = document.quizStatus,
            flashcardStatus = document.flashcardStatus,
            uploadDate = document.uploadDate.format(dateFormatter),
            updatedAt = document.updatedAt.format(dateFormatter)
        )
    }
    
    @Transactional(readOnly = true)
    fun listDocuments(userId: String): DocumentListResponse {
        val documents = documentRepository.findAllByUserIdOrderByUploadDateDesc(userId)
        
        val documentInfos = documents.map { document ->
            DocumentInfo(
                id = document.id,
                name = document.originalName,
                size = document.fileSize,
                uploadDate = document.uploadDate.format(dateFormatter),
                type = document.fileType ?: "unknown",
                status = document.status,
                summary = document.summary
            )
        }
        
        return DocumentListResponse(documents = documentInfos)
    }
    
    @Transactional(readOnly = true)
    fun getDocumentEntity(documentId: String, userId: String): DocumentEntity? {
        return documentRepository.findByUserIdAndId(userId, documentId)
    }
    
    @Transactional(readOnly = true)
    fun getDocumentEntityById(documentId: String): DocumentEntity? {
        return documentRepository.findById(documentId).orElse(null)
    }
    
    @Transactional(readOnly = true)
    fun getFileContent(documentId: String, userId: String): ByteArray {
        val document = documentRepository.findByUserIdAndId(userId, documentId)
            ?: throw RuntimeException("Document not found")
        
        return if (document.fileContent != null) {
            Base64.getDecoder().decode(document.fileContent)
        } else {
            throw RuntimeException("File content not available")
        }
    }
    

    
    private fun calculateProgress(document: DocumentEntity): Int {
        var completed = 0
        var total = 3
        
        if (document.summaryStatus == DocumentStatus.PROCESSED) completed++
        if (document.quizStatus == DocumentStatus.PROCESSED) completed++
        if (document.flashcardStatus == DocumentStatus.PROCESSED) completed++
        
        return (completed * 100) / total
    }
}

// Event class for document processing
data class DocumentProcessingEvent(
    val documentIds: List<String>,
    val userId: String
) 