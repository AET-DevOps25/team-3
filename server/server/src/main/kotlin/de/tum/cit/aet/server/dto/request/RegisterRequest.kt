package de.tum.cit.aet.server.dto.request

data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String,
    val role: String = "STUDENT"
)
