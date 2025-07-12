package de.tum.cit.aet.ai.service

import de.tum.cit.aet.ai.dto.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import reactor.core.publisher.Mono
import java.util.*

@Service
class AiService(
    private val webClient: WebClient
) {
    
    private val logger = LoggerFactory.getLogger(AiService::class.java)
    private val objectMapper = ObjectMapper()
    
    @Value("\${ai.openai.api.key}")
    private lateinit var openaiApiKey: String
    
    @Value("\${ai.openai.model}")
    private lateinit var openaiModel: String
    
    @Value("\${document.service.url}")
    private lateinit var documentServiceUrl: String
    
    fun processDocument(request: ProcessDocumentRequest): ProcessDocumentResponse {
        logger.info("Processing document: {}", request.documentId)
        
        return try {
            // Generate summary
            val summary = generateSummary(request.documentContent, request.documentName)
            
            // Generate processed content (key points, structure, etc.)
            val processedContent = generateProcessedContent(request.documentContent, request.documentName)
            
            // Update document service with results
            updateDocumentContent(request.documentId, summary, processedContent)
            
            ProcessDocumentResponse(
                documentId = request.documentId,
                status = "completed",
                summary = summary,
                processedContent = processedContent
            )
        } catch (e: Exception) {
            logger.error("Error processing document {}: {}", request.documentId, e.message, e)
            ProcessDocumentResponse(
                documentId = request.documentId,
                status = "failed",
                error = e.message
            )
        }
    }
    
    fun generateSummary(request: GenerateSummaryRequest): GenerateSummaryResponse {
        logger.info("Generating summary for document: {}", request.documentId)
        
        return try {
            val summary = generateSummary(request.documentContent, request.documentName)
            
            GenerateSummaryResponse(
                documentId = request.documentId,
                summary = summary,
                status = "completed"
            )
        } catch (e: Exception) {
            logger.error("Error generating summary for document {}: {}", request.documentId, e.message, e)
            GenerateSummaryResponse(
                documentId = request.documentId,
                summary = "",
                status = "failed",
                error = e.message
            )
        }
    }
    
    fun generateQuiz(request: GenerateQuizRequest): GenerateQuizResponse {
        logger.info("Generating quiz for document: {}", request.documentId)
        
        return try {
            val questions = generateQuizQuestions(request.documentContent, request.documentName, request.questionCount)
            
            GenerateQuizResponse(
                documentId = request.documentId,
                questions = questions,
                status = "completed"
            )
        } catch (e: Exception) {
            logger.error("Error generating quiz for document {}: {}", request.documentId, e.message, e)
            GenerateQuizResponse(
                documentId = request.documentId,
                questions = emptyList(),
                status = "failed",
                error = e.message
            )
        }
    }
    
    fun generateFlashcards(request: GenerateFlashcardsRequest): GenerateFlashcardsResponse {
        logger.info("Generating flashcards for document: {}", request.documentId)
        
        return try {
            val flashcards = generateFlashcards(request.documentContent, request.documentName, request.flashcardCount)
            
            GenerateFlashcardsResponse(
                documentId = request.documentId,
                flashcards = flashcards,
                status = "completed"
            )
        } catch (e: Exception) {
            logger.error("Error generating flashcards for document {}: {}", request.documentId, e.message, e)
            GenerateFlashcardsResponse(
                documentId = request.documentId,
                flashcards = emptyList(),
                status = "failed",
                error = e.message
            )
        }
    }
    
    private fun generateSummary(content: String, documentName: String): String {
        val prompt = """
            Please provide a comprehensive summary of the following document: "$documentName"
            
            Content:
            $content
            
            Please create a clear, well-structured summary that captures the main points, key concepts, and important details. The summary should be suitable for study purposes.
        """.trimIndent()
        
        return callOpenAI(prompt, 500)
    }
    
    private fun generateProcessedContent(content: String, documentName: String): JsonNode {
        val prompt = """
            Please analyze the following document: "$documentName"
            
            Content:
            $content
            
            Please provide a structured analysis including:
            1. Key concepts and definitions
            2. Main topics and subtopics
            3. Important facts and figures
            4. Relationships between concepts
            5. Study recommendations
            
            Return the analysis as a JSON object with these sections.
        """.trimIndent()
        
        val response = callOpenAI(prompt, 1000)
        return try {
            objectMapper.readTree(response)
        } catch (e: Exception) {
            // Fallback to simple structure if JSON parsing fails
            val fallback = objectMapper.createObjectNode()
            fallback.put("analysis", response)
            fallback.put("keyConcepts", "Extracted from document")
            fallback.put("mainTopics", "Identified from content")
            fallback
        }
    }
    
    private fun generateQuizQuestions(content: String, documentName: String, questionCount: Int): List<QuestionModel> {
        val prompt = """
            Please generate $questionCount quiz questions based on the following document: "$documentName"
            
            Content:
            $content
            
            Please create a mix of question types:
            - Multiple choice questions (with 4 options)
            - True/False questions
            - Short answer questions
            
            Return the questions as a JSON array with each question having:
            - type: "multiple_choice", "true_false", or "short_answer"
            - question: the question text
            - correctAnswer: the correct answer
            - points: points value (1-5)
            - options: array of options (for multiple choice only)
        """.trimIndent()
        
        val response = callOpenAI(prompt, 800)
        return try {
            val questionsArray = objectMapper.readTree(response)
            if (questionsArray.isArray) {
                questionsArray.map { questionNode ->
                    QuestionModel(
                        type = questionNode.get("type").asText(),
                        question = questionNode.get("question").asText(),
                        correctAnswer = questionNode.get("correctAnswer").asText(),
                        points = questionNode.get("points").asInt(),
                        options = if (questionNode.has("options")) {
                            questionNode.get("options").map { it.asText() }
                        } else null
                    )
                }
            } else {
                createFallbackQuestions(questionCount)
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse quiz questions, using fallback: {}", e.message)
            createFallbackQuestions(questionCount)
        }
    }
    
    private fun generateFlashcards(content: String, documentName: String, flashcardCount: Int): List<FlashcardModel> {
        val prompt = """
            Please generate $flashcardCount flashcards based on the following document: "$documentName"
            
            Content:
            $content
            
            Please create flashcards that cover key concepts, definitions, and important facts.
            Each flashcard should have:
            - question: a clear question or concept
            - answer: a concise answer or explanation
            - difficulty: "easy", "medium", or "hard"
            
            Return the flashcards as a JSON array.
        """.trimIndent()
        
        val response = callOpenAI(prompt, 600)
        return try {
            val flashcardsArray = objectMapper.readTree(response)
            if (flashcardsArray.isArray) {
                flashcardsArray.map { flashcardNode ->
                    FlashcardModel(
                        question = flashcardNode.get("question").asText(),
                        answer = flashcardNode.get("answer").asText(),
                        difficulty = flashcardNode.get("difficulty").asText()
                    )
                }
            } else {
                createFallbackFlashcards(flashcardCount)
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse flashcards, using fallback: {}", e.message)
            createFallbackFlashcards(flashcardCount)
        }
    }
    
    private fun callOpenAI(prompt: String, maxTokens: Int): String {
        // This is a simplified implementation
        // In a real implementation, you would make actual API calls to OpenAI
        logger.info("Calling OpenAI API with prompt length: {}", prompt.length)
        
        // Simulate API call delay
        Thread.sleep(1000)
        
        // Return a mock response for now
        return "This is a mock response from the AI service. In a real implementation, this would be the actual AI-generated content based on the provided prompt."
    }
    
    private fun updateDocumentContent(documentId: String, summary: String, processedContent: JsonNode) {
        try {
            webClient.post()
                .uri("$documentServiceUrl/api/documents/$documentId/content")
                .bodyValue(mapOf(
                    "summary" to summary,
                    "processedContent" to processedContent
                ))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
            
            logger.info("Updated document content for: {}", documentId)
        } catch (e: Exception) {
            logger.error("Failed to update document content for {}: {}", documentId, e.message)
        }
    }
    
    private fun createFallbackQuestions(count: Int): List<QuestionModel> {
        return (1..count).map { i ->
            QuestionModel(
                type = "multiple_choice",
                question = "Question $i about the document content",
                correctAnswer = "Correct answer $i",
                points = 1,
                options = listOf("Option A", "Option B", "Correct answer $i", "Option D")
            )
        }
    }
    
    private fun createFallbackFlashcards(count: Int): List<FlashcardModel> {
        return (1..count).map { i ->
            FlashcardModel(
                question = "Key concept $i",
                answer = "Explanation for concept $i",
                difficulty = if (i % 3 == 0) "hard" else if (i % 2 == 0) "medium" else "easy"
            )
        }
    }
} 