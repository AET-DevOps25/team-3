package com.team3.document.repository

import com.team3.document.entity.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DocumentRepository : JpaRepository<Document, UUID> 