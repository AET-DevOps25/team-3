package de.tum.cit.aet.document.repository

import de.tum.cit.aet.document.entity.DocumentEntity
import de.tum.cit.aet.document.dto.DocumentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<DocumentEntity, String> {
    
    // Find documents by user ID
    fun findByUserId(userId: String): List<DocumentEntity>
    
    // Find document by ID and user ID
    fun findByIdAndUserId(id: String, userId: String): DocumentEntity?
    
    // Check if document exists by ID and user ID
    fun existsByIdAndUserId(id: String, userId: String): Boolean
    
    // Find documents by user ID and status
    fun findByUserIdAndStatus(userId: String, status: DocumentStatus): List<DocumentEntity>
    
    // Get document count by user ID
    fun countByUserId(userId: String): Long
    
    // Find documents by user ID ordered by upload date
    fun findByUserIdOrderByUploadDateDesc(userId: String): List<DocumentEntity>
    
    // Find documents by user ID and file type
    fun findByUserIdAndFileType(userId: String, fileType: String): List<DocumentEntity>
    
    // Search documents by name for user ID
    @Query("SELECT d FROM DocumentEntity d WHERE d.userId = :userId AND LOWER(d.originalName) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByUserIdAndOriginalNameContainingIgnoreCase(@Param("userId") userId: String, @Param("name") name: String): List<DocumentEntity>
    
    // Find documents by status (for processing)
    fun findByStatus(status: DocumentStatus): List<DocumentEntity>
    
    // Find documents by summary status
    fun findBySummaryStatus(status: DocumentStatus): List<DocumentEntity>
    
    // Find documents by quiz status
    fun findByQuizStatus(status: DocumentStatus): List<DocumentEntity>
    
    // Find documents by flashcard status
    fun findByFlashcardStatus(status: DocumentStatus): List<DocumentEntity>
} 