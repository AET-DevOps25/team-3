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
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw IllegalArgumentException("Document not found with ID: $documentId")
        }

        // Get document info for context
        val document = documentRepository.findById(documentId).orElse(null)
        val documentName = document?.originalName ?: "Unknown Document"

        // Check quiz status first
        when (document?.quizStatus) {
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
                // Quiz is ready - return existing data if available
                if (document.quizData != null) {
                    val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                    try {
                        // Parse existing quiz data
                        val quizDataMap = objectMapper.readValue(document.quizData.toString(), Map::class.java) as Map<*, *>
                        val responseMap = quizDataMap["response"] as? Map<*, *>
                        val questionsList = responseMap?.get("questions") as? List<*>
                        
                        if (questionsList != null) {
                            val questions = questionsList.map { questionMap ->
                                val q = questionMap as Map<*, *>
                                QuestionModel(
                                    type = q["type"] as? String ?: "",
                                    question = q["question"] as? String ?: "",
                                    correctAnswer = q["correct_answer"] as? String ?: "",
                                    points = (q["points"] as? Number)?.toInt() ?: 0,
                                    options = (q["options"] as? List<*>)?.map { it.toString() }
                                )
                            }
                            return QuizApiResponse(
                                questions = questions,
                                documentName = documentName,
                                documentId = documentId,
                                status = "READY"
                            )
                        }
                    } catch (e: Exception) {
                        // If parsing fails, continue to generation below
                        println("Failed to parse existing quiz data: ${e.message}")
                    }
                }
                // If READY but no data, fall through to generate
            }
            DocumentStatus.UPLOADED -> {
                // Check if automatic document processing is still running
                // If so, wait for it to complete rather than starting individual generation
                if (document?.status == DocumentStatus.PROCESSING ||
                    document?.summaryStatus == DocumentStatus.PROCESSING ||
                    document?.flashcardStatus == DocumentStatus.PROCESSING) {
                    return QuizApiResponse(
                        questions = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is currently being processed. Quiz will be available soon."
                    )
                }
                
                // Check if this is a recently uploaded document that might still be in automatic processing
                // If document was uploaded recently and summary is not processed yet, wait for automatic processing
                if (document?.summaryStatus == DocumentStatus.UPLOADED) {
                    return QuizApiResponse(
                        questions = emptyList(),
                        documentName = documentName,
                        documentId = documentId,
                        status = "GENERATING",
                        error = "Document is being processed automatically. Quiz will be available soon."
                    )
                }
                
                // Only start individual generation if automatic processing has clearly completed without generating quiz
                // (i.e., summary is READY/ERROR but quiz is still UPLOADED)
            }
            else -> {
                // For null or other statuses, check if main document processing is running
                if (document?.status == DocumentStatus.PROCESSING) {
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

        // Only start generation if we reach here and automatic processing is not handling it
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
        // Check if document exists
        if (!documentRepository.existsById(documentId)) {
            throw IllegalArgumentException("Document not found with ID: $documentId")
        }

        // Get document info for context
        val document = documentRepository.findById(documentId).orElse(null)
        val documentName = document?.originalName ?: "Unknown Document"

        // Force regeneration by setting status to PROCESSING
        documentService.updateQuizStatus(documentId, DocumentStatus.PROCESSING)
        
        // Start async regeneration
        startAsyncQuizGeneration(documentId)
        
        // Return immediately with GENERATING status
        return QuizApiResponse(
            questions = emptyList(),
            documentName = documentName,
            documentId = documentId,
            status = "GENERATING",
            error = "Quiz is being regenerated. Please check back in a few moments."
        )
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