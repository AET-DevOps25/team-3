package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.entity.*
import de.tum.cit.aet.server.repository.ChatSessionRepository
import de.tum.cit.aet.server.repository.ChatMessageRepository
import de.tum.cit.aet.server.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class ChatService(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val documentRepository: DocumentRepository,
    private val genAiService: GenAiService
) {
    
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    fun createChatSession(request: ChatSessionRequest): ChatSessionResponse {
        val sessionId = UUID.randomUUID().toString()
        
        // Validate document IDs exist
        val validDocumentIds = request.documentIds.filter { documentId ->
            documentRepository.existsById(documentId)
        }
        
        val chatSession = ChatSessionEntity(
            id = sessionId,
            documentIds = validDocumentIds,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        chatSessionRepository.save(chatSession)
        
        // Create welcome message
        val welcomeMessage = createWelcomeMessage(chatSession, validDocumentIds)
        
        return ChatSessionResponse(
            sessionId = sessionId,
            messages = listOf(welcomeMessage),
            documentsInContext = validDocumentIds
        )
    }
    
    fun sendMessage(sessionId: String, request: ChatMessageRequest): ChatMessageResponse {
        val chatSession = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Chat session not found: $sessionId") }
        
        // Save user message
        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            content = request.message,
            sender = MessageSender.USER,
            timestamp = LocalDateTime.now(),
            chatSession = chatSession
        )
        chatMessageRepository.save(userMessage)
        
        // Generate AI response using GenAI service
        val aiResponse = generateAIResponse(request.message, chatSession.documentIds)
        val aiMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            content = aiResponse.content,
            sender = MessageSender.BOT,
            timestamp = LocalDateTime.now(),
            sources = aiResponse.sources,
            documentReferences = aiResponse.documentReferences,
            chatSession = chatSession
        )
        chatMessageRepository.save(aiMessage)
        
        // Update session timestamp
        chatSession.updatedAt = LocalDateTime.now()
        chatSessionRepository.save(chatSession)
        
        return ChatMessageResponse(
            id = aiMessage.id,
            content = aiMessage.content,
            sender = "bot",
            timestamp = aiMessage.timestamp,
            sources = aiMessage.sources,
            documentReferences = aiMessage.documentReferences.map { ref ->
                DocumentReference(
                    documentId = ref.documentId,
                    documentName = ref.documentName,
                    relevantPages = ref.relevantPages
                )
            }
        )
    }
    
    fun getChatSession(sessionId: String): ChatSessionResponse {
        val chatSession = chatSessionRepository.findByIdWithMessages(sessionId)
            ?: throw IllegalArgumentException("Chat session not found: $sessionId")
        
        val messages = chatMessageRepository.findByChatSessionIdOrderByTimestamp(sessionId)
            .map { message ->
                ChatMessageResponse(
                    id = message.id,
                    content = message.content,
                    sender = message.sender.name.lowercase(),
                    timestamp = message.timestamp,
                    sources = message.sources,
                    documentReferences = message.documentReferences.map { ref ->
                        DocumentReference(
                            documentId = ref.documentId,
                            documentName = ref.documentName,
                            relevantPages = ref.relevantPages
                        )
                    }
                )
            }
        
        return ChatSessionResponse(
            sessionId = sessionId,
            messages = messages,
            documentsInContext = chatSession.documentIds
        )
    }
    
    private fun createWelcomeMessage(chatSession: ChatSessionEntity, documentIds: List<String>): ChatMessageResponse {
        val documentNames = documentRepository.findAllById(documentIds)
            .map { it.originalName }
        
        val welcomeContent = if (documentNames.isNotEmpty()) {
            "Hi! I'm your AI study assistant. I've analyzed your uploaded documents: ${documentNames.joinToString(", ")}. " +
            "I'm ready to help you understand the concepts, clarify doubts, and answer questions based on these materials. What would you like to know?"
        } else {
            "Hi! I'm your AI study assistant. I'm ready to help you with your studies. What would you like to know?"
        }
        
        val welcomeMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            content = welcomeContent,
            sender = MessageSender.BOT,
            timestamp = LocalDateTime.now(),
            sources = documentNames,
            chatSession = chatSession
        )
        chatMessageRepository.save(welcomeMessage)
        
        return ChatMessageResponse(
            id = welcomeMessage.id,
            content = welcomeMessage.content,
            sender = "bot",
            timestamp = welcomeMessage.timestamp,
            sources = documentNames
        )
    }
    
    // AI response generator using GenAI service
    private fun generateAIResponse(userMessage: String, documentIds: List<String>): AIResponseData {
        // Get document names for context
        val documentNames = if (documentIds.isNotEmpty()) {
            documentRepository.findAllById(documentIds).map { it.originalName }
        } else {
            emptyList()
        }
        
        // Try to get AI response from GenAI service for the first document
        val aiContent = if (documentIds.isNotEmpty()) {
            val primaryDocumentId = documentIds.first()
            genAiService.chatWithDocument(primaryDocumentId, userMessage)
        } else {
            null
        }
        
        // Fallback to default response if GenAI service fails
        val content = aiContent ?: run {
            logger.warn("GenAI service failed, using fallback response")
            "I'm having trouble accessing the AI service right now. However, I can still help you with your studies! " +
            "Based on your uploaded documents (${documentNames.joinToString(", ")}), what specific topic would you like to discuss?"
        }
        
        return AIResponseData(
            content = content,
            sources = documentNames,
            documentReferences = documentIds.mapIndexed { index, docId ->
                DocumentReferenceData(
                    documentId = docId,
                    documentName = documentNames.getOrNull(index) ?: "Document $index",
                    relevantPages = listOf(1, 2) // Could be enhanced with actual page references
                )
            }
        )
    }
}

data class AIResponseData(
    val content: String,
    val sources: List<String> = emptyList(),
    val documentReferences: List<DocumentReferenceData> = emptyList()
) 