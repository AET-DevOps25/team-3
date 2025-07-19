package de.tum.cit.aet.document.controller

import de.tum.cit.aet.document.dto.*
import de.tum.cit.aet.document.entity.DocumentEntity
import de.tum.cit.aet.document.service.DocumentService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.userdetails.User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class DocumentControllerSimpleTest {

    @Mock
    private lateinit var documentService: DocumentService

    @InjectMocks
    private lateinit var documentController: DocumentController

    private val testUserId = "test-user-123"
    private val testDocumentId = "test-doc-123"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Test
    fun `uploadDocuments should return success response`() {
        // Given
        val file = MockMultipartFile("files", "test.pdf", "application/pdf", "test content".toByteArray())
        val expectedResponse = DocumentUploadResponse(
            documentIds = listOf(testDocumentId),
            status = "success",
            message = "Documents uploaded successfully"
        )
        val userDetails = User.builder().username(testUserId).password("password").roles("USER").build()
        whenever(documentService.uploadDocuments(any(), any())).thenReturn(expectedResponse)

        // When
        val result = documentController.uploadDocuments(listOf(file), userDetails)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals("success", result.body!!.status)
        assertEquals(1, result.body!!.documentIds.size)
        verify(documentService).uploadDocuments(any(), any())
    }

    @Test
    fun `getDocumentStatus should return document status`() {
        // Given
        val expectedResponse = DocumentStatusResponse(
            documentId = testDocumentId,
            status = DocumentStatus.UPLOADED,
            documentName = "test.pdf",
            uploadDate = LocalDateTime.now().format(dateFormatter),
            processingProgress = 0,
            summary = null
        )
        val userDetails = User.builder().username(testUserId).password("password").roles("USER").build()
        whenever(documentService.getDocumentStatus(testDocumentId, testUserId)).thenReturn(expectedResponse)

        // When
        val result = documentController.getDocumentStatus(testDocumentId, userDetails)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(testDocumentId, result.body!!.documentId)
        assertEquals(DocumentStatus.UPLOADED, result.body!!.status)
        verify(documentService).getDocumentStatus(testDocumentId, testUserId)
    }

    @Test
    fun `updateDocumentSummaryInternal should update summary`() {
        // Given
        val request = SummaryUpdateRequest(summary = "Updated summary")

        // When
        val result = documentController.updateDocumentSummaryInternal(testDocumentId, request)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Summary updated successfully", result.body)
        verify(documentService).updateDocumentSummary(testDocumentId, "Updated summary")
    }

    @Test
    fun `listDocuments should return document list`() {
        // Given
        val documentInfo = DocumentInfo(
            id = testDocumentId,
            name = "test.pdf",
            size = 1024L,
            uploadDate = LocalDateTime.now().format(dateFormatter),
            type = "application/pdf",
            status = DocumentStatus.UPLOADED,
            summary = null
        )
        val expectedResponse = DocumentListResponse(documents = listOf(documentInfo))
        val userDetails = User.builder().username(testUserId).password("password").roles("USER").build()
        whenever(documentService.listDocuments(testUserId)).thenReturn(expectedResponse)

        // When
        val result = documentController.listDocuments(userDetails)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(1, result.body!!.documents.size)
        assertEquals(testDocumentId, result.body!!.documents[0].id)
        verify(documentService).listDocuments(testUserId)
    }
}