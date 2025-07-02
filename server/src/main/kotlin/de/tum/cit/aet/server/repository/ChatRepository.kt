package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.entity.ChatSessionEntity
import de.tum.cit.aet.server.entity.ChatMessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChatSessionRepository : JpaRepository<ChatSessionEntity, String> {
    
    @Query("SELECT cs FROM ChatSessionEntity cs WHERE cs.updatedAt > :since ORDER BY cs.updatedAt DESC")
    fun findRecentSessions(@Param("since") since: LocalDateTime): List<ChatSessionEntity>
    
    @Query("SELECT cs FROM ChatSessionEntity cs JOIN cs.messages m WHERE cs.id = :sessionId")
    fun findByIdWithMessages(@Param("sessionId") sessionId: String): ChatSessionEntity?
}

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, String> {
    
    @Query("SELECT cm FROM ChatMessageEntity cm WHERE cm.chatSession.id = :sessionId ORDER BY cm.timestamp ASC")
    fun findByChatSessionIdOrderByTimestamp(@Param("sessionId") sessionId: String): List<ChatMessageEntity>
    
    @Query("SELECT cm FROM ChatMessageEntity cm WHERE cm.chatSession.id = :sessionId AND cm.timestamp > :since ORDER BY cm.timestamp ASC")
    fun findByChatSessionIdAndTimestampAfter(
        @Param("sessionId") sessionId: String, 
        @Param("since") since: LocalDateTime
    ): List<ChatMessageEntity>
} 