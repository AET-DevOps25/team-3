package de.tum.cit.aet.document.service

import de.tum.cit.aet.document.dto.DocumentStatus
import de.tum.cit.aet.document.entity.DocumentEntity
import de.tum.cit.aet.document.repository.DocumentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class DocumentServiceTest {

    @Mock
    private lateinit var documentRepository: DocumentRepository

    @Mock
    private lateinit var webClient: WebClient

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var documentService: DocumentService

    private lateinit var testDocument: DocumentEntity
    private val testUserId = "test-user-123"
    private val testDocumentId = "test-doc-123"

    @BeforeEach
    fun setUp() {
        testDocument = DocumentEntity(
            id = testDocumentId,
            originalName = "test.pdf",
            fileSize = 1024L,
            fileType = "application/pdf",
            fileContent = Base64.getEncoder().encodeToString("test content".toByteArray()),
            userId = testUserId
        )
    }

    @Test
    fun `uploadDocuments should save document and publish event`() {
        // Given
        val file = MockMultipartFile("file", "test.pdf", "application/pdf", "test content".toByteArray())
        whenever(documentRepository.save(any<DocumentEntity>())).thenReturn(testDocument)

        // When
        val result = documentService.uploadDocuments(listOf(file), testUserId)

        // Then
        assertNotNull(result)
        assertEquals("success", result.status)
        assertEquals(1, result.documentIds.size)
        verify(documentRepository).save(any<DocumentEntity>())
        verify(eventPublisher).publishEvent(any<DocumentProcessingEvent>())
    }

    @Test
    fun `updateDocumentSummary should update document summary and status`() {
        // Given
        val summary = "Test summary"
        whenever(documentRepository.findById(testDocumentId)).thenReturn(Optional.of(testDocument))
        whenever(documentRepository.save(any<DocumentEntity>())).thenReturn(testDocument)

        // When
        documentService.updateDocumentSummary(testDocumentId, summary)

        // Then
        assertEquals(summary, testDocument.summary)
        assertEquals(DocumentStatus.PROCESSED, testDocument.summaryStatus)
        verify(documentRepository).save(testDocument)
    }

    @Test
    fun `updateDocumentQuiz should update document quiz data and status`() {
        // Given
        val quizData = mapOf("questions" to listOf("What is the main topic?"))
        whenever(documentRepository.findById(testDocumentId)).thenReturn(Optional.of(testDocument))
        whenever(documentRepository.save(any<DocumentEntity>())).thenReturn(testDocument)

        // When
        documentService.updateDocumentQuiz(testDocumentId, quizData)

        // Then
        assertEquals(DocumentStatus.PROCESSED, testDocument.quizStatus)
        verify(documentRepository).save(testDocument)
    }

    @Test
    fun `updateDocumentFlashcards should update document flashcard data and status`() {
        // Given
        val flashcardData = listOf(mapOf("front" to "Question", "back" to "Answer"))
        whenever(documentRepository.findById(testDocumentId)).thenReturn(Optional.of(testDocument))
        whenever(documentRepository.save(any<DocumentEntity>())).thenReturn(testDocument)

        // When
        documentService.updateDocumentFlashcards(testDocumentId, flashcardData)

        // Then
        assertEquals(DocumentStatus.PROCESSED, testDocument.flashcardStatus)
        verify(documentRepository).save(testDocument)
    }

    @Test
    fun `getDocumentStatus should return document status response`() {
        // Given
        whenever(documentRepository.findByUserIdAndId(testUserId, testDocumentId)).thenReturn(testDocument)

        // When
        val result = documentService.getDocumentStatus(testDocumentId, testUserId)

        // Then
        assertNotNull(result)
        assertEquals(testDocumentId, result.documentId)
        assertEquals(DocumentStatus.UPLOADED, result.status)
        assertEquals("test.pdf", result.documentName)
    }

    @Test
    fun `getDocumentContent should return document content response`() {
        // Given
        whenever(documentRepository.findByUserIdAndId(testUserId, testDocumentId)).thenReturn(testDocument)

        // When
        val result = documentService.getDocumentContent(testDocumentId, testUserId)

        // Then
        assertNotNull(result)
        assertEquals(testDocumentId, result.id)
        assertEquals("test.pdf", result.originalName)
        assertEquals(DocumentStatus.UPLOADED, result.status)
    }

    @Test
    fun `listDocuments should return document list response`() {
        // Given
        whenever(documentRepository.findAllByUserIdOrderByUploadDateDesc(testUserId)).thenReturn(listOf(testDocument))

        // When
        val result = documentService.listDocuments(testUserId)

        // Then
        assertNotNull(result)
        assertEquals(1, result.documents.size)
        assertEquals(testDocumentId, result.documents[0].id)
        assertEquals("test.pdf", result.documents[0].name)
    }

    @Test
    fun `getDocumentEntity should return document entity`() {
        // Given
        whenever(documentRepository.findByUserIdAndId(testUserId, testDocumentId)).thenReturn(testDocument)

        // When
        val result = documentService.getDocumentEntity(testDocumentId, testUserId)

        // Then
        assertNotNull(result)
        assertEquals(testDocumentId, result!!.id)
        assertEquals("test.pdf", result.originalName)
    }

    @Test
    fun `getFileContent should return decoded file content`() {
        // Given
        whenever(documentRepository.findByUserIdAndId(testUserId, testDocumentId)).thenReturn(testDocument)

        // When
        val result = documentService.getFileContent(testDocumentId, testUserId)

        // Then
        assertNotNull(result)
        assertEquals("test content", String(result))
    }
}