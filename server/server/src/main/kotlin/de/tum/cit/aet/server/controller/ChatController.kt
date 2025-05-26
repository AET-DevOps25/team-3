package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.service.ChatService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {
    // TODO: Implement chat endpoints
    // - POST /message (send chat message)
    // - GET /history/{documentId} (get chat history)
    // - DELETE /history/{documentId} (clear chat history)
}
