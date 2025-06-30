package de.tum.cit.aet.server.entity

import de.tum.cit.aet.server.dto.DocumentStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.JsonNode
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "documents")
data class DocumentEntity(
    @Id
    val id: String,
    
    // Original file info
    @Column(name = "original_name", nullable = false)
    val originalName: String,
    
    @Column(name = "file_size", nullable = false)
    val fileSize: Long,
    
    @Column(name = "file_type")
    val fileType: String?,
    
    // Raw file content (for download)
    @Lob
    @Column(name = "file_content")
    val fileContent: String?, // Base64 encoded, nullable after processing
    
    // AI-processed content
    @Column(name = "summary", columnDefinition = "text")
    var summary: String? = null,
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "processed_content")
    var processedContent: JsonNode? = null, // Extracted text, key points, etc.
    
    // Status and timing
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: DocumentStatus = DocumentStatus.UPLOADED,
    
    @Column(name = "upload_date", nullable = false)
    val uploadDate: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    
    // Optional metadata
    @Column(name = "page_count")
    val pageCount: Int? = null
)
