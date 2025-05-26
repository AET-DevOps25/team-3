package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.repository.ChatMessageRepository
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatMessageRepository: ChatMessageRepository,
    private val aiService: AIService
) {
    // TODO: Implement chat service methods
}
