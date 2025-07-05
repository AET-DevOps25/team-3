package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.ChatMessageRequest
import de.tum.cit.aet.server.dto.ChatMessageResponse
import de.tum.cit.aet.server.dto.ChatSessionRequest
import de.tum.cit.aet.server.dto.ChatSessionResponse
import de.tum.cit.aet.server.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = ["*"])
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/sessions")
    fun createChatSession(@RequestBody request: ChatSessionRequest): ResponseEntity<ChatSessionResponse> {
        val response = chatService.createChatSession(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/sessions/{sessionId}")
    fun getChatSession(@PathVariable sessionId: String): ResponseEntity<ChatSessionResponse> {
        return try {
            val response = chatService.getChatSession(sessionId)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/sessions/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: String,
        @RequestBody request: ChatMessageRequest
    ): ResponseEntity<ChatMessageResponse> {
        return try {
            val response = chatService.sendMessage(sessionId, request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
} 