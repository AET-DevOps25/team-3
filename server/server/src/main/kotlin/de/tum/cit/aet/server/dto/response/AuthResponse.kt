package de.tum.cit.aet.server.dto.response

data class AuthResponse(
    val token: String,
    val user: UserResponse
)
