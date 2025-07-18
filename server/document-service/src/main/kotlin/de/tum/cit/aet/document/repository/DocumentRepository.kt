package de.tum.cit.aet.document.repository

import de.tum.cit.aet.document.entity.DocumentEntity
import de.tum.cit.aet.document.dto.DocumentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DocumentRepository : JpaRepository<DocumentEntity, String> {
    
    fun findByUserId(userId: String): List<DocumentEntity>
    
    fun findByUserIdAndStatus(userId: String, status: DocumentStatus): List<DocumentEntity>
    
    fun findByUserIdAndId(userId: String, id: String): DocumentEntity?
    
    @Query("SELECT d FROM DocumentEntity d WHERE d.userId = :userId ORDER BY d.uploadDate DESC")
    fun findAllByUserIdOrderByUploadDateDesc(@Param("userId") userId: String): List<DocumentEntity>
    
    @Query("SELECT COUNT(d) FROM DocumentEntity d WHERE d.userId = :userId")
    fun countByUserId(@Param("userId") userId: String): Long
    
    @Query("SELECT COUNT(d) FROM DocumentEntity d WHERE d.userId = :userId AND d.status = :status")
    fun countByUserIdAndStatus(@Param("userId") userId: String, @Param("status") status: DocumentStatus): Long
    
    fun deleteByUserIdAndId(userId: String, id: String): Long
} 