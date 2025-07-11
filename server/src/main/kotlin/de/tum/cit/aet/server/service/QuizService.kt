package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.QuizApiResponse
import de.tum.cit.aet.server.dto.QuestionModel
import de.tum.cit.aet.server.dto.DocumentStatus
import de.tum.cit.aet.server.repository.DocumentRepository
import org.springframework.stereotype.Service

@Service
class QuizService(
    private val genAiService: GenAiService,
    private val documentRepository: DocumentRepository,
    private val documentService: DocumentService
) {

    fun generateQuiz(documentId: String): QuizApiResponse {
        // Use utility method for validation
        val document = documentService.validateDocumentExists(documentId)
        val documentName = document.originalName

        // Check quiz status first
        when (document.quizStatus) {
            DocumentStatus.PROCESSING -> {
                return QuizApiResponse(
                    questions = emptyList(),
                    documentName = documentName,
                    documentId = documentId,
                    status = "GENERATING",
                    error = "Quiz is currently being generated. Please try again in a few moments."
                )
            }
            DocumentStatus.ERROR -> {
                return QuizApiResponse(
                    questions = generateFallbackQuiz(documentName),
                    documentName = documentName,
                    documentId = documentId,
                    status = "FAILED",
                    error = "Quiz generation failed. Showing fallback questions."
                )
            }
            DocumentStatus.READY -> {
                // Use utility method for JSON parsing
                val questions = documentService.parseJsonContent(document.quizData) { responseMap ->
                    val questionsList = responseMap["questions"] as? List<*>
                    questionsList?.map { questionMap ->
                        val q = questionMap as Map<*, *>
                        QuestionModel(
                            type = q["type"] as? String ?: "",
                            question = q["question"] as? String ?: "",
                            correctAnswer = q["correct_answer"] as? String ?: "",
                            points = (q["points"] as? Number)?.toInt() ?: 0,
                            options = (q["options"] as? List<*>)?.map { it.toString() }
                        )
                    } ?: emptyList()
                }
                
                if (!questions.isNullOrEmpty()) {
                    return QuizApiResponse(
                        questions = questions,
                        documentName = documentName,
                        documentId = documentId,
                        status = "READY"
                    )
                }
            }
            DocumentStatus.UPLOADED -> {
                // Use utility method for auto-processing check
                if (documentService.shouldWaitForAutoProcessing(document)) {
                    return QuizApiResponse(
                        questions = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is being processed automatically. Quiz will be available soon."
                    )
                }
            }
            else -> {
                if (document.status == DocumentStatus.PROCESSING) {
                    return QuizApiResponse(
                        questions = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is currently being processed. Quiz will be available soon."
                    )
                }
            }
        }

        return startQuizGeneration(documentId, documentName)
    }

    private fun startQuizGeneration(documentId: String, documentName: String): QuizApiResponse {
        // Set status to PROCESSING to indicate generation has started
        documentService.updateQuizStatus(documentId, DocumentStatus.PROCESSING)
        
        // Start async generation using existing background processing
        // The actual generation will happen in the background via GenAI service
        startAsyncQuizGeneration(documentId)
        
        // Return immediately with GENERATING status so frontend can poll
        return QuizApiResponse(
            questions = emptyList(),
            documentName = documentName,
            documentId = documentId,
            status = "GENERATING",
            error = "Quiz is being generated. Please check back in a few moments."
        )
    }
    
    private fun startAsyncQuizGeneration(documentId: String) {
        // Use a separate thread to avoid blocking the API response
        Thread {
            try {
                val questions = genAiService.generateQuiz(documentId)
                
                if (questions != null && questions.isNotEmpty()) {
                    // Update the database with new quiz data
                    val quizResponse = de.tum.cit.aet.server.dto.QuizResponse(
                        de.tum.cit.aet.server.dto.QuizData(questions = questions)
                    )
                    documentService.updateDocumentQuizData(documentId, quizResponse)
                    documentService.updateQuizStatus(documentId, DocumentStatus.READY)
                } else {
                    // Mark as failed
                    documentService.updateQuizStatus(documentId, DocumentStatus.ERROR)
                }
            } catch (e: Exception) {
                // Mark as failed on any error
                documentService.updateQuizStatus(documentId, DocumentStatus.ERROR)
            }
        }.start()
    }

    fun regenerateQuiz(documentId: String): QuizApiResponse {
        // Use utility method for validation
        val documentName = documentService.getDocumentNameSafely(documentId)
        
        // Force regeneration by directly calling generation method
        return startQuizGeneration(documentId, documentName)
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