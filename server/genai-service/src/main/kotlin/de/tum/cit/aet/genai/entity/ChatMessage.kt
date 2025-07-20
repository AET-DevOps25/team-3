package de.tum.cit.aet.genai.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_messages")
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(name = "session_id", nullable = false)
    val sessionId: String,
    
    @Column(name = "user_id", nullable = false)
    val userId: String,
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false)
    val sender: MessageSender,
    
    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "document_ids")
    val documentIds: String? = null  // JSON string of document IDs
)

enum class MessageSender {
    USER, BOT
} 