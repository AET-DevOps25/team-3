package de.tum.cit.aet.server.entity

import de.tum.cit.aet.server.dto.FileStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "uploaded_files")
data class FileEntity(
    @Id
    val id: String,
    
    @Column(name = "original_name", nullable = false)
    val originalName: String,
    
    @Column(name = "file_size", nullable = false)
    val fileSize: Long,
    
    @Column(name = "file_type")
    val fileType: String?,
    
    @Lob
    @Column(name = "file_content", nullable = false)
    val fileContent: String, // Base64 encoded content
    
    @Column(name = "summary")
    val summary: String? = null,
    
    @Column(name = "upload_date", nullable = false)
    val uploadDate: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "page_count")
    val pageCount: Int? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: FileStatus = FileStatus.UPLOADED
) {
}
