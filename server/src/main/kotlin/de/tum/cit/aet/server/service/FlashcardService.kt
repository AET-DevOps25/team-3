package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.FlashcardApiResponse
import de.tum.cit.aet.server.dto.FlashcardModel
import de.tum.cit.aet.server.dto.DocumentStatus
import de.tum.cit.aet.server.repository.DocumentRepository
import org.springframework.stereotype.Service

@Service
class FlashcardService(
    private val genAiService: GenAiService,
    private val documentRepository: DocumentRepository,
    private val documentService: DocumentService
) {

    fun generateFlashcards(documentId: String): FlashcardApiResponse {
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw IllegalArgumentException("Document not found with ID: $documentId")
        }

        // Get document info for context
        val document = documentRepository.findById(documentId).orElse(null)
        val documentName = document?.originalName ?: "Unknown Document"

        // Check flashcard status first
        when (document?.flashcardStatus) {
            DocumentStatus.PROCESSING -> {
                return FlashcardApiResponse(
                    flashcards = emptyList(),
                    documentName = documentName,
                    documentId = documentId,
                    status = "GENERATING",
                    error = "Flashcards are currently being generated. Please try again in a few moments."
                )
            }
            DocumentStatus.ERROR -> {
                return FlashcardApiResponse(
                    flashcards = generateFallbackFlashcards(documentName),
                    documentName = documentName,
                    documentId = documentId,
                    status = "FAILED",
                    error = "Flashcard generation failed. Showing fallback flashcards."
                )
            }
            DocumentStatus.READY -> {
                // Flashcards are ready, return cached data
                if (document.flashcardData != null) {
                    try {
                        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                        // Parse as Map to avoid Jackson constructor issues
                        val flashcardDataMap = objectMapper.readValue(document.flashcardData.toString(), Map::class.java) as Map<*, *>
                        val responseMap = flashcardDataMap["response"] as? Map<*, *>
                        val flashcardsList = responseMap?.get("flashcards") as? List<*>
                        
                        if (flashcardsList != null) {
                            val flashcards = flashcardsList.map { flashcardMap ->
                                val fc = flashcardMap as Map<*, *>
                                FlashcardModel(
                                    question = fc["question"] as? String ?: "",
                                    answer = fc["answer"] as? String ?: "",
                                    difficulty = fc["difficulty"] as? String ?: "medium"
                                )
                            }
                            
                            return FlashcardApiResponse(
                                flashcards = flashcards,
                                documentName = documentName,
                                documentId = documentId,
                                status = "READY"
                            )
                        } else {
                            println("Failed to parse flashcard data: no flashcards found")
                        }
                    } catch (e: Exception) {
                        // If parsing fails, fall back to generating new flashcards
                        println("Failed to parse existing flashcard data: ${e.message}")
                    }
                }
                // If READY but no data, fall through to generation
            }
            DocumentStatus.UPLOADED -> {
                // Check if automatic document processing is still running
                // If so, wait for it to complete rather than starting individual generation
                if (document?.status == DocumentStatus.PROCESSING ||
                    document?.summaryStatus == DocumentStatus.PROCESSING ||
                    document?.quizStatus == DocumentStatus.PROCESSING) {
                    return FlashcardApiResponse(
                        flashcards = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is currently being processed. Flashcards will be available soon."
                    )
                }
                
                // Check if this is a recently uploaded document that might still be in automatic processing
                // If document was uploaded recently and summary is not processed yet, wait for automatic processing
                if (document?.summaryStatus == DocumentStatus.UPLOADED) {
                    return FlashcardApiResponse(
                        flashcards = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is being processed automatically. Flashcards will be available soon."
                    )
                }
                
                // Only start individual generation if automatic processing has clearly completed without generating flashcards
                // (i.e., summary is READY/ERROR but flashcards are still UPLOADED)
            }
            else -> {
                // For null or other statuses, check if main document processing is running
                if (document?.status == DocumentStatus.PROCESSING) {
                    return FlashcardApiResponse(
                        flashcards = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is currently being processed. Flashcards will be available soon."
                    )
                }
            }
        }

        // Only start generation if we reach here and automatic processing is not handling it
        return startFlashcardGeneration(documentId, documentName)
    }

    private fun startFlashcardGeneration(documentId: String, documentName: String): FlashcardApiResponse {
        // Set status to PROCESSING
        documentService.updateFlashcardStatus(documentId, DocumentStatus.PROCESSING)
        
        // Generate flashcards using GenAI service
        val flashcards = genAiService.generateFlashcards(documentId)
        
        if (flashcards != null && flashcards.isNotEmpty()) {
            // Update the database with new flashcard data
            val flashcardResponse = de.tum.cit.aet.server.dto.FlashcardResponse(
                de.tum.cit.aet.server.dto.FlashcardsData(flashcards = flashcards)
            )
            documentService.updateDocumentFlashcardData(documentId, flashcardResponse)
            documentService.updateFlashcardStatus(documentId, DocumentStatus.READY)
            
            return FlashcardApiResponse(
                flashcards = flashcards,
                documentName = documentName,
                documentId = documentId,
                status = "READY"
            )
        } else {
            // Mark as failed and return fallback
            documentService.updateFlashcardStatus(documentId, DocumentStatus.ERROR)
            return FlashcardApiResponse(
                flashcards = generateFallbackFlashcards(documentName),
                documentName = documentName,
                documentId = documentId,
                status = "FAILED",
                error = "AI flashcard generation failed, showing fallback flashcards"
            )
        }
    }

    fun regenerateFlashcards(documentId: String): FlashcardApiResponse {
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw IllegalArgumentException("Document not found with ID: $documentId")
        }

        // Get document info for context
        val document = documentRepository.findById(documentId).orElse(null)
        val documentName = document?.originalName ?: "Unknown Document"

        // Force regeneration by setting status to PROCESSING
        documentService.updateFlashcardStatus(documentId, DocumentStatus.PROCESSING)
        
        // Generate flashcards using GenAI service
        val flashcards = genAiService.generateFlashcards(documentId)
        
        if (flashcards != null && flashcards.isNotEmpty()) {
            // Update the database with new flashcard data
            val flashcardResponse = de.tum.cit.aet.server.dto.FlashcardResponse(
                de.tum.cit.aet.server.dto.FlashcardsData(flashcards = flashcards)
            )
            documentService.updateDocumentFlashcardData(documentId, flashcardResponse)
            documentService.updateFlashcardStatus(documentId, DocumentStatus.READY)
            
            return FlashcardApiResponse(
                flashcards = flashcards,
                documentName = documentName,
                documentId = documentId,
                status = "READY"
            )
        } else {
            // Mark as failed and return fallback
            documentService.updateFlashcardStatus(documentId, DocumentStatus.ERROR)
            return FlashcardApiResponse(
                flashcards = generateFallbackFlashcards(documentName),
                documentName = documentName,
                documentId = documentId,
                status = "FAILED",
                error = "AI flashcard generation failed, showing fallback flashcards"
            )
        }
    }

    private fun generateFallbackFlashcards(documentName: String): List<FlashcardModel> {
        return listOf(
            FlashcardModel(
                question = "What is the main topic covered in this document?",
                answer = "This document covers various topics that you should review thoroughly.",
                difficulty = "easy"
            ),
            FlashcardModel(
                question = "What are the key concepts from this document?",
                answer = "Please review the document content and identify the key concepts for yourself.",
                difficulty = "medium"
            ),
            FlashcardModel(
                question = "How would you summarize the main points of this document?",
                answer = "Create your own summary based on your understanding of the document content.",
                difficulty = "hard"
            )
        )
    }
} 