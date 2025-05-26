package de.tum.cit.aet.server.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val filePath: String,

    @Column(nullable = false)
    val fileName: String,

    @Enumerated(EnumType.STRING)
    val contentType: DocumentType,

    @Enumerated(EnumType.STRING)
    val status: DocumentStatus = DocumentStatus.PROCESSING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @Column(nullable = false)
    val uploadDate: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL])
    val summaries: List<Summary> = emptyList(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL])
    val quizzes: List<Quiz> = emptyList(),

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL])
    val flashcards: List<Flashcard> = emptyList()
)

enum class DocumentType {
    PDF,
    PPT,
    PPTX,
    TXT
}

enum class DocumentStatus {
    PROCESSING,
    READY,
    FAILED
}
