package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.ChatMessageRequest
import de.tum.cit.aet.server.dto.ChatMessageResponse
import de.tum.cit.aet.server.dto.ChatSessionRequest
import de.tum.cit.aet.server.dto.ChatSessionResponse
import de.tum.cit.aet.server.entity.UserEntity
import de.tum.cit.aet.server.service.ChatService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = ["*"])
class ChatController(
    private val chatService: ChatService
) {
    
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    @PostMapping("/sessions")
    fun createChatSession(
        @RequestBody request: ChatSessionRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<ChatSessionResponse> {
        logger.info("POST /api/chat/sessions/")
        val response = chatService.createChatSession(request, user)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/sessions/{sessionId}")
    fun getChatSession(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<ChatSessionResponse> {
        return try {
            logger.info("GET /api/chat/sessions/{}", sessionId)
            val response = chatService.getChatSession(sessionId, user)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/sessions/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: String,
        @RequestBody request: ChatMessageRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<ChatMessageResponse> {
        return try {
            logger.info("POST /api/chat/sessions/{}/messages", sessionId)
            val response = chatService.sendMessage(sessionId, request, user)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
} 