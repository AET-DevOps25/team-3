package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.entity.DocumentEntity
import de.tum.cit.aet.server.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<DocumentEntity, String> {
    
    // Find documents by user
    fun findByUser(user: UserEntity): List<DocumentEntity>
    
    // Find document by ID and user
    fun findByIdAndUser(id: String, user: UserEntity): DocumentEntity?
    
    // Check if document exists by ID and user
    fun existsByIdAndUser(id: String, user: UserEntity): Boolean
    
    // Find documents by user and status
    fun findByUserAndStatus(user: UserEntity, status: de.tum.cit.aet.server.dto.DocumentStatus): List<DocumentEntity>
    
    // Get document count by user
    fun countByUser(user: UserEntity): Long
    
    // Find documents by user ordered by upload date
    fun findByUserOrderByUploadDateDesc(user: UserEntity): List<DocumentEntity>
    
    // Find documents by user and file type
    fun findByUserAndFileType(user: UserEntity, fileType: String): List<DocumentEntity>
    
    // Search documents by name for user
    @Query("SELECT d FROM DocumentEntity d WHERE d.user = :user AND LOWER(d.originalName) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByUserAndOriginalNameContainingIgnoreCase(@Param("user") user: UserEntity, @Param("name") name: String): List<DocumentEntity>
} 