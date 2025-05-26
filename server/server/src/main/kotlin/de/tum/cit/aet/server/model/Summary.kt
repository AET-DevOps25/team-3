package de.tum.cit.aet.server.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "summaries")
data class Summary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
