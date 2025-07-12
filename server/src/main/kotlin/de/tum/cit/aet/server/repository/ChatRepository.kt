package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.entity.ChatSessionEntity
import de.tum.cit.aet.server.entity.ChatMessageEntity
import de.tum.cit.aet.server.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChatSessionRepository : JpaRepository<ChatSessionEntity, String> {
    
    // Find chat sessions by user
    fun findByUser(user: UserEntity): List<ChatSessionEntity>
    
    // Find chat session by ID and user
    fun findByIdAndUser(id: String, user: UserEntity): ChatSessionEntity?
    
    // Find chat sessions by user ordered by update date
    fun findByUserOrderByUpdatedAtDesc(user: UserEntity): List<ChatSessionEntity>
    
    // Get chat session count by user
    fun countByUser(user: UserEntity): Long
    
    // Find recent chat sessions by user
    @Query("SELECT c FROM ChatSessionEntity c WHERE c.user = :user ORDER BY c.updatedAt DESC")
    fun findRecentByUser(@Param("user") user: UserEntity): List<ChatSessionEntity>
}

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, String> {
    
    // Find messages by chat session ID ordered by timestamp
    fun findByChatSessionIdOrderByTimestamp(chatSessionId: String): List<ChatMessageEntity>
    
    // Find messages by chat session
    fun findByChatSessionOrderByTimestamp(chatSession: ChatSessionEntity): List<ChatMessageEntity>
    
    // Find messages by user (through chat session)
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.chatSession.user = :user ORDER BY m.timestamp DESC")
    fun findByUserOrderByTimestampDesc(@Param("user") user: UserEntity): List<ChatMessageEntity>
    
    // Get message count by user
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.chatSession.user = :user")
    fun countByUser(@Param("user") user: UserEntity): Long
    
    // Find messages by user and sender
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.chatSession.user = :user AND m.sender = :sender ORDER BY m.timestamp DESC")
    fun findByUserAndSenderOrderByTimestampDesc(@Param("user") user: UserEntity, @Param("sender") sender: de.tum.cit.aet.server.entity.MessageSender): List<ChatMessageEntity>
} 