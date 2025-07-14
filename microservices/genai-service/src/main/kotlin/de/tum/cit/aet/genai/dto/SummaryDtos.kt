package de.tum.cit.aet.genai.dto

import jakarta.validation.constraints.NotBlank

data class SummaryGenerationRequest(
    @field:NotBlank(message = "Document ID is required")
    val documentId: String,
    
    @field:NotBlank(message = "User ID is required")
    val userId: String
)

data class SummaryUpdateRequest(
    @field:NotBlank(message = "Summary is required")
    val summary: String
)

data class SummaryResponse(
    val summary: String?,
    val documentId: String? = null,
    val status: String = "success",
    val error: String? = null
)