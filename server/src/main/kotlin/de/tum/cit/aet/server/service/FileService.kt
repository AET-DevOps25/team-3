package de.tum.cit.aet.server.service

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.entity.FileEntity
import de.tum.cit.aet.server.repository.FileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class FileService(private val fileRepository: FileRepository) {
    
    @Transactional
    fun uploadFiles(files: Array<MultipartFile>): FileUploadResponse {
        val fileIds = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        for (file in files) {
            try {
                // Validate file
                if (file.isEmpty) {
                    errors.add("File ${file.originalFilename} is empty")
                    continue
                }
                
                // Generate unique ID
                val fileId = UUID.randomUUID().toString()
                
                // Convert file to Base64
                val base64Content = Base64.getEncoder().encodeToString(file.bytes)
                
                // Create and save file entity
                val fileEntity = FileEntity(
                    id = fileId,
                    originalName = file.originalFilename ?: "unknown",
                    fileSize = file.size,
                    fileType = extractFileType(file.originalFilename ?: ""),
                    fileContent = base64Content,
                    uploadDate = LocalDateTime.now(),
                    status = FileStatus.UPLOADED
                )
                
                fileRepository.save(fileEntity)
                fileIds.add(fileId)
                
            } catch (e: Exception) {
                errors.add("Error uploading file ${file.originalFilename}: ${e.message}")
            }
        }
        
        // If all files failed, throw exception
        if (fileIds.isEmpty() && errors.isNotEmpty()) {
            throw RuntimeException("All file uploads failed: ${errors.joinToString(", ")}")
        }
        
        return FileUploadResponse(
            fileIds = fileIds,
            status = if (errors.isEmpty()) "success" else "partial_success"
        )
    }
    
    fun getFileStatus(fileId: String): FileStatusResponse {
        val fileEntity = fileRepository.findById(fileId)
            .orElseThrow { RuntimeException("File not found with ID: $fileId") }
        
        return FileStatusResponse(
            fileId = fileEntity.id,
            status = fileEntity.status,
            fileName = fileEntity.originalName,
            uploadDate = fileEntity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
        )
    }
    
    fun getFileEntity(fileId: String): FileEntity? {
        return fileRepository.findById(fileId).orElse(null)
    }
    
    fun getFileContent(fileId: String): ByteArray {
        val fileEntity = fileRepository.findById(fileId)
            .orElseThrow { RuntimeException("File not found with ID: $fileId") }
        
        return Base64.getDecoder().decode(fileEntity.fileContent)
    }
    
    fun listFiles(): FileListResponse {
        val files = fileRepository.findAll().map { entity ->
            FileInfo(
                id = entity.id,
                name = entity.originalName,
                size = entity.fileSize,
                uploadDate = entity.uploadDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z",
                type = entity.fileType ?: "unknown"
            )
        }
        
        return FileListResponse(files = files)
    }
    
    @Transactional
    fun deleteFile(fileId: String) {
        if (!fileRepository.existsById(fileId)) {
            throw RuntimeException("File not found with ID: $fileId")
        }
        // TODO: Delete associated data (summaries, quizzes, etc.)
        // For now, just delete the file
        fileRepository.deleteById(fileId)
    }
    
    private fun extractFileType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }
    
} 