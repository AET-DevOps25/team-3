package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.model.Document
import de.tum.cit.aet.server.model.Quiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuizRepository : JpaRepository<Quiz, Long> {
    fun findByDocument(document: Document): List<Quiz>
    fun findByDocumentId(documentId: Long): List<Quiz>
}
