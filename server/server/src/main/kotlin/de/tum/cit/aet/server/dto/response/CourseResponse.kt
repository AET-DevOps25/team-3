package de.tum.cit.aet.server.dto.response

import java.time.LocalDateTime

data class CourseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val instructor: UserResponse,
    val createdAt: LocalDateTime,
    val documentsCount: Int = 0,
    val enrolledStudentsCount: Int = 0
)
