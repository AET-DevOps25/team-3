package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.model.Document
import de.tum.cit.aet.server.model.Summary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SummaryRepository : JpaRepository<Summary, Long> {
    fun findByDocument(document: Document): Optional<Summary>
    fun findByDocumentId(documentId: Long): Optional<Summary>
}
