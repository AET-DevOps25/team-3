package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.model.Course
import de.tum.cit.aet.server.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseRepository : JpaRepository<Course, Long> {
    fun findByInstructor(instructor: User): List<Course>
    fun findByEnrolledStudentsContaining(student: User): List<Course>
}
