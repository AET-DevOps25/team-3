package de.tum.cit.aet.auth.controller

import de.tum.cit.aet.auth.dto.UserRegistrationRequest
import de.tum.cit.aet.auth.dto.UserLoginRequest
import de.tum.cit.aet.auth.dto.JwtAuthenticationResponse
import de.tum.cit.aet.auth.dto.UserResponse
import de.tum.cit.aet.auth.entity.Role
import de.tum.cit.aet.auth.service.AuthService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.BindingResult
import java.time.LocalDateTime

class AuthControllerTest {

    private lateinit var authController: AuthController
    private lateinit var authService: AuthService
    private lateinit var bindingResult: BindingResult

    @BeforeEach
    fun setUp() {
        authService = mock()
        bindingResult = mock()
        authController = AuthController(authService)
    }

    @Test
    fun `should register user successfully`() {
        // Given
        val request = UserRegistrationRequest("testuser", "test@example.com", "password123", "Test", "User")
        val userResponse = UserResponse(
            id = "1",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            fullName = "Test User",
            role = Role.USER,
            isActive = true,
            createdAt = LocalDateTime.now(),
            lastLoginAt = null
        )
        val authResponse = JwtAuthenticationResponse(
            token = "jwt-token",
            refreshToken = "refresh-token",
            expiresIn = 3600000L,
            user = userResponse
        )
        
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(authService.register(request)).thenReturn(authResponse)
        
        // When
        val result = authController.register(request, bindingResult)
        
        // Then
        assertEquals(200, result.statusCode.value())
        verify(authService).register(request)
    }

    @Test
    fun `should login user successfully`() {
        // Given
        val request = UserLoginRequest("testuser", "password123")
        val userResponse = UserResponse(
            id = "1",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            fullName = "Test User",
            role = Role.USER,
            isActive = true,
            createdAt = LocalDateTime.now(),
            lastLoginAt = null
        )
        val authResponse = JwtAuthenticationResponse(
            token = "jwt-token",
            refreshToken = "refresh-token",
            expiresIn = 3600000L,
            user = userResponse
        )
        
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(authService.login(request)).thenReturn(authResponse)
        
        // When
        val result = authController.login(request, bindingResult)
        
        // Then
        assertEquals(200, result.statusCode.value())
        verify(authService).login(request)
    }

    @Test
    fun `should return 401 for invalid credentials`() {
        // Given
        val request = UserLoginRequest("testuser", "wrongpassword")
        
        whenever(bindingResult.hasErrors()).thenReturn(false)
        whenever(authService.login(request)).thenThrow(UsernameNotFoundException("User not found"))
        
        // When
        val result = authController.login(request, bindingResult)
        
        // Then
        assertEquals(401, result.statusCode.value())
        verify(authService).login(request)
    }

    @Test
    fun `should return 400 for invalid registration data`() {
        // Given
        val request = UserRegistrationRequest("", "test@example.com", "password123", "Test", "User")
        
        whenever(bindingResult.hasErrors()).thenReturn(true)
        whenever(bindingResult.fieldErrors).thenReturn(emptyList())
        
        // When
        val result = authController.register(request, bindingResult)
        
        // Then
        assertEquals(400, result.statusCode.value())
    }
}