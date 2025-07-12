package de.tum.cit.aet.server.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "chat_sessions")
data class ChatSessionEntity(
    @Id
    val id: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_ids", columnDefinition = "jsonb")
    val documentIds: List<String> = emptyList(),
    
    @OneToMany(mappedBy = "chatSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val messages: MutableList<ChatMessageEntity> = mutableListOf(),
    
    // User relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity
)

@Entity
@Table(name = "chat_messages")
data class ChatMessageEntity(
    @Id
    val id: String,
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false)
    val sender: MessageSender,
    
    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources", columnDefinition = "jsonb")
    val sources: List<String> = emptyList(),
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_references", columnDefinition = "jsonb")
    val documentReferences: List<DocumentReferenceData> = emptyList(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    val chatSession: ChatSessionEntity
)

enum class MessageSender {
    USER, BOT
}

data class DocumentReferenceData(
    val documentId: String,
    val documentName: String,
    val relevantPages: List<Int> = emptyList()
) 