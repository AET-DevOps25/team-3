package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.service.FileService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = ["*"])
class FileController(private val fileService: FileService) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFiles(@RequestParam("files") files: Array<MultipartFile>): ResponseEntity<Any> {
        return try {
            if (files.isEmpty()) {
                return ResponseEntity.badRequest().body(mapOf("error" to "No files provided"))
            }
            
            val response = fileService.uploadFiles(files)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{fileId}/status")
    fun getFileStatus(@PathVariable fileId: String): ResponseEntity<Any> {
        return try {
            val response = fileService.getFileStatus(fileId)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun listFiles(): ResponseEntity<FileListResponse> {
        return ResponseEntity.ok(fileService.listFiles())
    }

    @GetMapping("/{fileId}/download")
    fun downloadFile(@PathVariable fileId: String): ResponseEntity<Any> {
        return try {
            val fileEntity = fileService.getFileEntity(fileId)
                ?: return ResponseEntity.notFound().build()
            
            val fileContent = fileService.getFileContent(fileId)
            
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileEntity.originalName}\"")
                .contentType(MediaType.parseMediaType(fileEntity.fileType ?: "application/octet-stream"))
                .contentLength(fileContent.size.toLong())
                .body(fileContent)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{fileId}")
    fun deleteFile(@PathVariable fileId: String): ResponseEntity<Any> {
        return try {
            fileService.deleteFile(fileId)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
} 