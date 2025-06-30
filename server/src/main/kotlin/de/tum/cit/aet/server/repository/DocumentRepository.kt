package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.entity.DocumentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<DocumentEntity, String> 