package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.model.Course
import de.tum.cit.aet.server.model.Document
import de.tum.cit.aet.server.model.DocumentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    fun findByCourse(course: Course): List<Document>
    fun findByStatus(status: DocumentStatus): List<Document>
    fun findByCourseId(courseId: Long): List<Document>
}
