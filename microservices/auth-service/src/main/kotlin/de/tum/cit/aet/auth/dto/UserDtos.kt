package de.tum.cit.aet.auth.dto

import de.tum.cit.aet.auth.entity.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// Registration DTOs
data class UserRegistrationRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    val username: String,
    
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email should be valid")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val password: String,
    
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 50, message = "First name cannot exceed 50 characters")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 50, message = "Last name cannot exceed 50 characters")
    val lastName: String
)

// Login DTOs
data class UserLoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String
)

// Response DTOs
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val role: Role,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime?
)

data class JwtAuthenticationResponse(
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse
)

data class JwtRefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

data class JwtRefreshResponse(
    val token: String,
    val refreshToken: String,
    val expiresIn: Long
)

// Update DTOs
data class UserUpdateRequest(
    @field:Size(max = 50, message = "First name cannot exceed 50 characters")
    val firstName: String?,
    
    @field:Size(max = 50, message = "Last name cannot exceed 50 characters")
    val lastName: String?,
    
    @field:Email(message = "Email should be valid")
    val email: String?
)

data class UserPasswordChangeRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val newPassword: String
)

// List and search DTOs
data class UserListResponse(
    val users: List<UserResponse>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

data class UserSearchRequest(
    val query: String?,
    val role: Role?,
    val isActive: Boolean?,
    val page: Int = 0,
    val pageSize: Int = 20
)

// Error response DTOs
data class AuthErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ValidationErrorResponse(
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) 