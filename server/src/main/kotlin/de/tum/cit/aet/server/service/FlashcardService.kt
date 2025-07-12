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
        // Use utility method for validation
        val document = documentService.validateDocumentExists(documentId)
        val documentName = document.originalName

        // Check flashcard status first
        when (document.flashcardStatus) {
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
                // Use utility method for JSON parsing
                val flashcards = documentService.parseJsonContent(document.flashcardData) { responseMap ->
                    val flashcardsList = responseMap["flashcards"] as? List<*>
                    flashcardsList?.map { flashcardMap ->
                        val fc = flashcardMap as Map<*, *>
                        FlashcardModel(
                            question = fc["question"] as? String ?: "",
                            answer = fc["answer"] as? String ?: "",
                            difficulty = fc["difficulty"] as? String ?: "medium"
                        )
                    } ?: emptyList()
                }
                
                if (!flashcards.isNullOrEmpty()) {
                    return FlashcardApiResponse(
                        flashcards = flashcards,
                        documentName = documentName,
                        documentId = documentId,
                        status = "READY"
                    )
                }
            }
            DocumentStatus.UPLOADED -> {
                // Use utility method for auto-processing check
                if (documentService.shouldWaitForAutoProcessing(document)) {
                    return FlashcardApiResponse(
                        flashcards = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is being processed automatically. Flashcards will be available soon."
                    )
                }
            }
            else -> {
                if (document.status == DocumentStatus.PROCESSING) {
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
        // Use utility method for validation
        val documentName = documentService.getDocumentNameSafely(documentId)
        
        // Force regeneration by directly calling generation method
        return startFlashcardGeneration(documentId, documentName)
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