package de.tum.cit.aet.auth.service

import de.tum.cit.aet.auth.entity.UserEntity
import de.tum.cit.aet.auth.entity.Role
import de.tum.cit.aet.auth.repository.UserRepository
import de.tum.cit.aet.auth.dto.UserRegistrationRequest
import de.tum.cit.aet.auth.dto.UserLoginRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        passwordEncoder = mock()
        jwtService = mock()
        authService = AuthService(userRepository, passwordEncoder, jwtService)
    }

    @Test
    fun `should create user successfully`() {
        // Given
        val request = UserRegistrationRequest("testuser", "test@example.com", "password123", "Test", "User")
        val encodedPassword = "encodedPassword123"
        val userEntity = UserEntity(
            id = "1",
            username = "testuser",
            email = "test@example.com",
            password = encodedPassword,
            firstName = "Test",
            lastName = "User",
            role = Role.USER,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(userRepository.existsByUsername("testuser")).thenReturn(false)
        whenever(userRepository.existsByEmail("test@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("password123")).thenReturn(encodedPassword)
        whenever(userRepository.save(any<UserEntity>())).thenReturn(userEntity)
        whenever(jwtService.generateToken(userEntity)).thenReturn("jwt-token")
        whenever(jwtService.generateRefreshToken(userEntity)).thenReturn("refresh-token")
        whenever(jwtService.getExpirationTime()).thenReturn(3600000L)
        
        // When
        val result = authService.register(request)
        
        // Then
        assertNotNull(result)
        assertEquals("jwt-token", result.token)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals("testuser", result.user.username)
        assertEquals("test@example.com", result.user.email)
        
        verify(userRepository).existsByUsername("testuser")
        verify(userRepository).existsByEmail("test@example.com")
        verify(passwordEncoder).encode("password123")
        verify(userRepository).save(any<UserEntity>())
    }

    @Test
    fun `should not create user with duplicate username`() {
        // Given
        val request = UserRegistrationRequest("testuser", "test@example.com", "password123", "Test", "User")
        
        whenever(userRepository.existsByUsername("testuser")).thenReturn(true)
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            authService.register(request)
        }
        
        verify(userRepository).existsByUsername("testuser")
    }

    @Test
    fun `should authenticate user with correct credentials`() {
        // Given
        val request = UserLoginRequest("testuser", "password123")
        val userEntity = UserEntity(
            id = "1",
            username = "testuser",
            email = "test@example.com",
            password = "encodedPassword123",
            firstName = "Test",
            lastName = "User",
            role = Role.USER,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(userRepository.findByUsernameAndIsActive("testuser", true)).thenReturn(userEntity)
        whenever(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true)
        whenever(userRepository.save(any<UserEntity>())).thenReturn(userEntity)
        whenever(jwtService.generateToken(userEntity)).thenReturn("jwt-token")
        whenever(jwtService.generateRefreshToken(userEntity)).thenReturn("refresh-token")
        whenever(jwtService.getExpirationTime()).thenReturn(3600000L)
        
        // When
        val result = authService.login(request)
        
        // Then
        assertNotNull(result)
        assertEquals("jwt-token", result.token)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals("testuser", result.user.username)
        
        verify(userRepository).findByUsernameAndIsActive("testuser", true)
        verify(passwordEncoder).matches("password123", "encodedPassword123")
    }

    @Test
    fun `should not authenticate user with incorrect credentials`() {
        // Given
        val request = UserLoginRequest("testuser", "wrongpassword")
        val userEntity = UserEntity(
            id = "1",
            username = "testuser",
            email = "test@example.com",
            password = "encodedPassword123",
            firstName = "Test",
            lastName = "User",
            role = Role.USER,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(userRepository.findByUsernameAndIsActive("testuser", true)).thenReturn(userEntity)
        whenever(passwordEncoder.matches("wrongpassword", "encodedPassword123")).thenReturn(false)
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            authService.login(request)
        }
        
        verify(userRepository).findByUsernameAndIsActive("testuser", true)
        verify(passwordEncoder).matches("wrongpassword", "encodedPassword123")
    }

    @Test
    fun `should not authenticate non-existent user`() {
        // Given
        val request = UserLoginRequest("nonexistent", "password123")
        
        whenever(userRepository.findByUsernameAndIsActive("nonexistent", true)).thenReturn(null)
        
        // When & Then
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException::class.java) {
            authService.login(request)
        }
        
        verify(userRepository).findByUsernameAndIsActive("nonexistent", true)
    }
}