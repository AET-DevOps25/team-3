package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.service.CourseService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseService: CourseService
) {
    // TODO: Implement course endpoints
    // - POST / (create course)
    // - GET /{id} (get course)
    // - GET / (list courses)
    // - PUT /{id} (update course)
    // - DELETE /{id} (delete course)
    // - POST /{id}/enroll (enroll student)
}
