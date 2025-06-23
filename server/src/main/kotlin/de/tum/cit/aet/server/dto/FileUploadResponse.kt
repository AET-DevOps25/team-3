package de.tum.cit.aet.server.dto

data class FileUploadResponse(
    val fileIds: List<String>,
    val status: String
)

data class FileStatusResponse(
    val fileId: String,
    val status: FileStatus,
    val fileName: String,
    val uploadDate: String,
    val processingProgress: Int = 0
)

enum class FileStatus {
    UPLOADING,
    UPLOADED,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class FileMetadata(
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int? = null
)

data class FileListResponse(
    val files: List<FileInfo>
)

data class FileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val uploadDate: String,
    val type: String
) 