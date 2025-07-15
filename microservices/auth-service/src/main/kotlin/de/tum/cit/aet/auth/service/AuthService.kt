package de.tum.cit.aet.auth.service

import de.tum.cit.aet.auth.dto.*
import de.tum.cit.aet.auth.entity.UserEntity
import de.tum.cit.aet.auth.entity.Role
import de.tum.cit.aet.auth.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    
    @Transactional
    fun register(request: UserRegistrationRequest): JwtAuthenticationResponse {
        logger.info("Registering new user: {}", request.username)
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }
        
        // Create new user
        val user = UserEntity(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            role = Role.USER,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(user)
        
        // Generate tokens
        val jwtToken = jwtService.generateToken(savedUser)
        val refreshToken = jwtService.generateRefreshToken(savedUser)
        
        logger.info("User registered successfully: {}", savedUser.username)
        
        return JwtAuthenticationResponse(
            token = jwtToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getExpirationTime(),
            user = convertToUserResponse(savedUser)
        )
    }
    
    @Transactional
    fun login(request: UserLoginRequest): JwtAuthenticationResponse {
        logger.info("User login attempt: {}", request.username)
        
        // Load user details
        val user = userRepository.findByUsernameAndIsActive(request.username, true)
            ?: throw UsernameNotFoundException("User not found or inactive")
        
        // Verify password
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Invalid password")
        }
        
        // Update last login
        user.updateLastLogin()
        userRepository.save(user)
        
        // Generate tokens
        val jwtToken = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)
        
        logger.info("User logged in successfully: {}", user.username)
        
        return JwtAuthenticationResponse(
            token = jwtToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getExpirationTime(),
            user = convertToUserResponse(user)
        )
    }
    
    @Transactional
    fun refreshToken(request: JwtRefreshRequest): JwtRefreshResponse {
        val username = jwtService.extractUsername(request.refreshToken)
        val user = userRepository.findByUsernameAndIsActive(username, true)
            ?: throw UsernameNotFoundException("User not found")
        
        if (!jwtService.isRefreshTokenValid(request.refreshToken, user)) {
            throw SecurityException("Invalid refresh token")
        }
        
        val jwtToken = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)
        
        return JwtRefreshResponse(
            token = jwtToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getExpirationTime()
        )
    }
    
    @Transactional(readOnly = true)
    fun getCurrentUser(username: String): UserResponse {
        val user = userRepository.findByUsernameAndIsActive(username, true)
            ?: throw UsernameNotFoundException("User not found")
        
        return convertToUserResponse(user)
    }
    
    @Transactional
    fun updateUser(username: String, request: UserUpdateRequest): UserResponse {
        val user = userRepository.findByUsernameAndIsActive(username, true)
            ?: throw UsernameNotFoundException("User not found")
        
        // Check if email is being changed and if it's already taken
        if (request.email != null && request.email != user.email) {
            if (userRepository.existsByEmail(request.email)) {
                throw IllegalArgumentException("Email already exists")
            }
        }
        
        // Update user fields
        val updatedUser = user.copy(
            firstName = request.firstName ?: user.firstName,
            lastName = request.lastName ?: user.lastName,
            email = request.email ?: user.email,
            updatedAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(updatedUser)
        logger.info("User updated successfully: {}", savedUser.username)
        
        return convertToUserResponse(savedUser)
    }
    
    @Transactional
    fun changePassword(username: String, request: UserPasswordChangeRequest): Boolean {
        val user = userRepository.findByUsernameAndIsActive(username, true)
            ?: throw UsernameNotFoundException("User not found")
        
        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw IllegalArgumentException("Current password is incorrect")
        }
        
        // Update password
        val updatedUser = user.copy(
            password = passwordEncoder.encode(request.newPassword),
            updatedAt = LocalDateTime.now()
        )
        
        userRepository.save(updatedUser)
        logger.info("Password changed successfully for user: {}", user.username)
        
        return true
    }
    
    @Transactional
    fun deactivateUser(username: String): Boolean {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found")
        
        val updatedUser = user.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )
        
        userRepository.save(updatedUser)
        logger.info("User deactivated: {}", user.username)
        
        return true
    }
    
    @Transactional(readOnly = true)
    fun searchUsers(request: UserSearchRequest): UserListResponse {
        val pageable = PageRequest.of(request.page, request.pageSize)
        val userPage = userRepository.searchUsers(
            request.query,
            request.role,
            request.isActive,
            pageable
        )
        
        val users = userPage.content.map { convertToUserResponse(it) }
        
        return UserListResponse(
            users = users,
            totalCount = userPage.totalElements.toInt(),
            page = request.page,
            pageSize = request.pageSize
        )
    }
    
    @Transactional(readOnly = true)
    fun getUserById(id: String): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { UsernameNotFoundException("User not found with id: $id") }
        
        return convertToUserResponse(user)
    }
    
    @Transactional(readOnly = true)
    fun getUserStats(): Map<String, Any> {
        val totalUsers = userRepository.count()
        val activeUsers = userRepository.countActiveUsers()
        val adminUsers = userRepository.countByRoleAndIsActive(Role.ADMIN)
        val regularUsers = userRepository.countByRoleAndIsActive(Role.USER)
        
        return mapOf(
            "totalUsers" to totalUsers,
            "activeUsers" to activeUsers,
            "adminUsers" to adminUsers,
            "regularUsers" to regularUsers
        )
    }
    
    private fun convertToUserResponse(user: UserEntity): UserResponse {
        return UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            fullName = user.getFullName(),
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }
} 