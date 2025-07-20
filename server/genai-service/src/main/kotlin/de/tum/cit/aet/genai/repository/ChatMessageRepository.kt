package de.tum.cit.aet.genai.repository

import de.tum.cit.aet.genai.entity.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, String> {
    
    // Get messages for a specific session, ordered by timestamp
    fun findBySessionIdOrderByTimestampAsc(sessionId: String): List<ChatMessage>
    
    // Get messages for a user across all sessions
    fun findByUserIdOrderByTimestampDesc(userId: String): List<ChatMessage>
    
    // Get recent messages for a session (limit)
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = :sessionId ORDER BY m.timestamp DESC LIMIT :limit")
    fun findRecentMessagesBySessionId(@Param("sessionId") sessionId: String, @Param("limit") limit: Int): List<ChatMessage>
    
    // Count messages in a session
    fun countBySessionId(sessionId: String): Long
    
    // Delete all messages for a session
    fun deleteBySessionId(sessionId: String): Long
    
    // Check if session exists (has any messages)
    fun existsBySessionId(sessionId: String): Boolean
} 