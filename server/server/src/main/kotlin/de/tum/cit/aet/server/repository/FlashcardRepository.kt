package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.model.Document
import de.tum.cit.aet.server.model.Flashcard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FlashcardRepository : JpaRepository<Flashcard, Long> {
    fun findByDocument(document: Document): List<Flashcard>
    fun findByDocumentId(documentId: Long): List<Flashcard>
}
