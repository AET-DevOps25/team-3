package de.tum.cit.aet.server.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "flashcards")
data class Flashcard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val term: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val definition: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
