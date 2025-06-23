package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.dto.FileStatus
import de.tum.cit.aet.server.entity.FileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FileRepository : JpaRepository<FileEntity, String> {
    
    fun findByOriginalNameContainingIgnoreCase(name: String): List<FileEntity>
    
    fun findByFileType(fileType: String): List<FileEntity>
    
    fun findByStatus(status: FileStatus): List<FileEntity>
    
    fun findByOriginalName(originalName: String): List<FileEntity>
} 