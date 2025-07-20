package de.tum.cit.aet.genai.service

import de.tum.cit.aet.genai.dto.ChatMessageDto
import de.tum.cit.aet.genai.entity.ChatMessage
import de.tum.cit.aet.genai.entity.MessageSender
import de.tum.cit.aet.genai.repository.ChatMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ChatMessageService(
    private val chatMessageRepository: ChatMessageRepository
) {
    
    private val logger = LoggerFactory.getLogger(ChatMessageService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    fun saveUserMessage(sessionId: String, userId: String, content: String, documentIds: List<String>? = null): ChatMessage {
        logger.info("Saving user message for session: {} user: {}", sessionId, userId)
        
        val message = ChatMessage(
            sessionId = sessionId,
            userId = userId,
            content = content,
            sender = MessageSender.USER,
            documentIds = documentIds?.joinToString(",")
        )
        
        return chatMessageRepository.save(message)
    }
    
    fun saveBotMessage(sessionId: String, userId: String, content: String): ChatMessage {
        logger.info("Saving bot message for session: {} user: {}", sessionId, userId)
        
        val message = ChatMessage(
            sessionId = sessionId,
            userId = userId,
            content = content,
            sender = MessageSender.BOT
        )
        
        return chatMessageRepository.save(message)
    }
    
    @Transactional(readOnly = true)
    fun getSessionMessages(sessionId: String): List<ChatMessageDto> {
        logger.info("Retrieving messages for session: {}", sessionId)
        
        val messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId)
        
        return messages.map { message ->
            ChatMessageDto(
                id = message.id ?: "",
                content = message.content,
                sender = message.sender.name.lowercase(),
                timestamp = message.timestamp.format(dateFormatter),
                sources = null,
                documentReferences = null
            )
        }
    }
    
    @Transactional(readOnly = true)
    fun getRecentMessages(sessionId: String, limit: Int = 50): List<ChatMessageDto> {
        logger.info("Retrieving recent {} messages for session: {}", limit, sessionId)
        
        val messages = chatMessageRepository.findRecentMessagesBySessionId(sessionId, limit)
            .reversed() // Reverse to get chronological order
        
        return messages.map { message ->
            ChatMessageDto(
                id = message.id ?: "",
                content = message.content,
                sender = message.sender.name.lowercase(),
                timestamp = message.timestamp.format(dateFormatter),
                sources = null,
                documentReferences = null
            )
        }
    }
    
    @Transactional(readOnly = true)
    fun sessionExists(sessionId: String): Boolean {
        return chatMessageRepository.existsBySessionId(sessionId)
    }
    
    @Transactional(readOnly = true)
    fun getMessageCount(sessionId: String): Long {
        return chatMessageRepository.countBySessionId(sessionId)
    }
    
    fun clearSessionMessages(sessionId: String): Long {
        logger.info("Clearing all messages for session: {}", sessionId)
        return chatMessageRepository.deleteBySessionId(sessionId)
    }
} 