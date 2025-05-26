package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.model.ChatMessage
import de.tum.cit.aet.server.model.Document
import de.tum.cit.aet.server.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    fun findByUserAndDocument(user: User, document: Document): List<ChatMessage>
    fun findByDocumentId(documentId: Long): List<ChatMessage>
    fun findByUserId(userId: Long): List<ChatMessage>
}
