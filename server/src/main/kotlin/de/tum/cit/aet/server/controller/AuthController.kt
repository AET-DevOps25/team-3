package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.dto.*
import de.tum.cit.aet.server.service.AuthService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
class AuthController(
    private val authService: AuthService
) {
    
    private val logger = LoggerFactory.getLogger(AuthController::class.java)
    
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: UserRegistrationRequest,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        logger.info("POST /api/auth/register - username: {}", request.username)
        
        if (bindingResult.hasErrors()) {
            val fieldErrors = bindingResult.fieldErrors.associate { 
                it.field to (it.defaultMessage ?: "Invalid value")
            }
            return ResponseEntity.badRequest().body(
                ValidationErrorResponse(
                    error = "Validation failed",
                    message = "Request validation failed",
                    fieldErrors = fieldErrors
                )
            )
        }
        
        return try {
            val response = authService.register(request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                AuthErrorResponse(
                    error = "Registration failed",
                    message = e.message ?: "Unknown error"
                )
            )
        } catch (e: Exception) {
            logger.error("Registration error for username {}: {}", request.username, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Registration failed",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: UserLoginRequest,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        logger.info("POST /api/auth/login - username: {}", request.username)
        
        if (bindingResult.hasErrors()) {
            val fieldErrors = bindingResult.fieldErrors.associate { 
                it.field to (it.defaultMessage ?: "Invalid value")
            }
            return ResponseEntity.badRequest().body(
                ValidationErrorResponse(
                    error = "Validation failed",
                    message = "Request validation failed",
                    fieldErrors = fieldErrors
                )
            )
        }
        
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(response)
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthErrorResponse(
                    error = "Authentication failed",
                    message = "Invalid username or password"
                )
            )
        } catch (e: UsernameNotFoundException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthErrorResponse(
                    error = "Authentication failed",
                    message = "Invalid username or password"
                )
            )
        } catch (e: Exception) {
            logger.error("Login error for username {}: {}", request.username, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Authentication failed",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: JwtRefreshRequest,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        logger.info("POST /api/auth/refresh")
        
        if (bindingResult.hasErrors()) {
            val fieldErrors = bindingResult.fieldErrors.associate { 
                it.field to (it.defaultMessage ?: "Invalid value")
            }
            return ResponseEntity.badRequest().body(
                ValidationErrorResponse(
                    error = "Validation failed",
                    message = "Request validation failed",
                    fieldErrors = fieldErrors
                )
            )
        }
        
        return try {
            val response = authService.refreshToken(request)
            ResponseEntity.ok(response)
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthErrorResponse(
                    error = "Token refresh failed",
                    message = "Invalid refresh token"
                )
            )
        } catch (e: UsernameNotFoundException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthErrorResponse(
                    error = "Token refresh failed",
                    message = "User not found"
                )
            )
        } catch (e: Exception) {
            logger.error("Token refresh error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Token refresh failed",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<Any> {
        logger.info("GET /api/auth/me - user: {}", authentication.name)
        
        return try {
            val userResponse = authService.getCurrentUser(authentication.name)
            ResponseEntity.ok(userResponse)
        } catch (e: UsernameNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AuthErrorResponse(
                    error = "User not found",
                    message = "Current user not found"
                )
            )
        } catch (e: Exception) {
            logger.error("Get current user error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Failed to get user",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    fun updateCurrentUser(
        @Valid @RequestBody request: UserUpdateRequest,
        authentication: Authentication,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        logger.info("PUT /api/auth/me - user: {}", authentication.name)
        
        if (bindingResult.hasErrors()) {
            val fieldErrors = bindingResult.fieldErrors.associate { 
                it.field to (it.defaultMessage ?: "Invalid value")
            }
            return ResponseEntity.badRequest().body(
                ValidationErrorResponse(
                    error = "Validation failed",
                    message = "Request validation failed",
                    fieldErrors = fieldErrors
                )
            )
        }
        
        return try {
            val userResponse = authService.updateUser(authentication.name, request)
            ResponseEntity.ok(userResponse)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                AuthErrorResponse(
                    error = "Update failed",
                    message = e.message ?: "Unknown error"
                )
            )
        } catch (e: UsernameNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AuthErrorResponse(
                    error = "User not found",
                    message = "Current user not found"
                )
            )
        } catch (e: Exception) {
            logger.error("Update user error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Update failed",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @PutMapping("/me/password")
    @PreAuthorize("hasRole('USER')")
    fun changePassword(
        @Valid @RequestBody request: UserPasswordChangeRequest,
        authentication: Authentication,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        logger.info("PUT /api/auth/me/password - user: {}", authentication.name)
        
        if (bindingResult.hasErrors()) {
            val fieldErrors = bindingResult.fieldErrors.associate { 
                it.field to (it.defaultMessage ?: "Invalid value")
            }
            return ResponseEntity.badRequest().body(
                ValidationErrorResponse(
                    error = "Validation failed",
                    message = "Request validation failed",
                    fieldErrors = fieldErrors
                )
            )
        }
        
        return try {
            authService.changePassword(authentication.name, request)
            ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                AuthErrorResponse(
                    error = "Password change failed",
                    message = e.message ?: "Unknown error"
                )
            )
        } catch (e: UsernameNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AuthErrorResponse(
                    error = "User not found",
                    message = "Current user not found"
                )
            )
        } catch (e: Exception) {
            logger.error("Change password error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Password change failed",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @PostMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    fun searchUsers(
        @RequestBody request: UserSearchRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/auth/users/search")
        
        return try {
            val response = authService.searchUsers(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Search users error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Search failed",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserById(@PathVariable id: String): ResponseEntity<Any> {
        logger.info("GET /api/auth/users/{}", id)
        
        return try {
            val userResponse = authService.getUserById(id)
            ResponseEntity.ok(userResponse)
        } catch (e: UsernameNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AuthErrorResponse(
                    error = "User not found",
                    message = "User not found with id: $id"
                )
            )
        } catch (e: Exception) {
            logger.error("Get user by id error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Failed to get user",
                    message = "An internal error occurred"
                )
            )
        }
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserStats(): ResponseEntity<Any> {
        logger.info("GET /api/auth/stats")
        
        return try {
            val stats = authService.getUserStats()
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            logger.error("Get user stats error: {}", e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthErrorResponse(
                    error = "Failed to get stats",
                    message = "An internal error occurred"
                )
            )
        }
    }
} 