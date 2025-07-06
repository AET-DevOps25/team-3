package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.QuizApiResponse
import de.tum.cit.aet.server.dto.QuestionModel
import de.tum.cit.aet.server.repository.DocumentRepository
import org.springframework.stereotype.Service

@Service
class QuizService(
    private val genAiService: GenAiService,
    private val documentRepository: DocumentRepository
) {

    fun generateQuiz(documentId: String): QuizApiResponse {
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw IllegalArgumentException("Document not found with ID: $documentId")
        }

        // Get document info for context
        val document = documentRepository.findById(documentId).orElse(null)
        val documentName = document?.originalName ?: "Unknown Document"

        // Generate quiz using GenAI service
        val questions = genAiService.generateQuiz(documentId)
        
        if (questions != null && questions.isNotEmpty()) {
            return QuizApiResponse(
                questions = questions,
                documentName = documentName,
                documentId = documentId
            )
        } else {
            // Return fallback quiz if GenAI fails
            return QuizApiResponse(
                questions = generateFallbackQuiz(documentName),
                documentName = documentName,
                documentId = documentId,
                error = "AI quiz generation failed, showing fallback questions"
            )
        }
    }

    private fun generateFallbackQuiz(documentName: String): List<QuestionModel> {
        return listOf(
            QuestionModel(
                type = "mcq",
                question = "What is the main topic covered in this document?",
                correctAnswer = "This document covers various topics that you should review.",
                points = 1,
                options = listOf(
                    "This document covers various topics that you should review.",
                    "The document is empty.",
                    "It's a technical manual.",
                    "It's a fiction story."
                )
            ),
            QuestionModel(
                type = "short",
                question = "Summarize the key concepts from this document in your own words.",
                correctAnswer = "Please review the document content and provide your own summary of the key concepts.",
                points = 3
            )
        )
    }
} 