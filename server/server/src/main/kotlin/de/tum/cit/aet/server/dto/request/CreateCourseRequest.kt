package de.tum.cit.aet.server.dto.request

data class CreateCourseRequest(
    val name: String,
    val description: String?
)
