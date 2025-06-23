package com.team3.document.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false, columnDefinition = "jsonb")
    var content: String,

    @Column(nullable = false, columnDefinition = "jsonb")
    var metadata: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) 