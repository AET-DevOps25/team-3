package de.tum.cit.aet.server.dto.request

data class LoginRequest(
    val email: String,
    val password: String
)
