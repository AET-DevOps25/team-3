package de.tum.cit.aet.server.dto.response

import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: LocalDateTime
)
