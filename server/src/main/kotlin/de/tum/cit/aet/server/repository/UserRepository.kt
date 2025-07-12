package de.tum.cit.aet.server.repository

import de.tum.cit.aet.server.entity.UserEntity
import de.tum.cit.aet.server.entity.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {
    
    // Authentication queries
    fun findByUsername(username: String): UserEntity?
    
    fun findByEmail(email: String): UserEntity?
    
    fun findByUsernameOrEmail(username: String, email: String): UserEntity?
    
    // Existence checks
    fun existsByUsername(username: String): Boolean
    
    fun existsByEmail(email: String): Boolean
    
    fun existsByUsernameOrEmail(username: String, email: String): Boolean
    
    // Active user queries
    fun findByUsernameAndIsActive(username: String, isActive: Boolean): UserEntity?
    
    fun findByEmailAndIsActive(email: String, isActive: Boolean): UserEntity?
    
    @Query("SELECT u FROM UserEntity u WHERE u.isActive = :isActive")
    fun findAllByIsActive(@Param("isActive") isActive: Boolean): List<UserEntity>
    
    @Query("SELECT u FROM UserEntity u WHERE u.isActive = :isActive")
    fun findAllByIsActive(@Param("isActive") isActive: Boolean, pageable: Pageable): Page<UserEntity>
    
    // Role queries
    fun findByRole(role: Role): List<UserEntity>
    
    fun findByRole(role: Role, pageable: Pageable): Page<UserEntity>
    
    fun findByRoleAndIsActive(role: Role, isActive: Boolean): List<UserEntity>
    
    fun findByRoleAndIsActive(role: Role, isActive: Boolean, pageable: Pageable): Page<UserEntity>
    
    // Search queries
    @Query("""
        SELECT u FROM UserEntity u 
        WHERE (:query IS NULL OR 
               LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:role IS NULL OR u.role = :role)
        AND (:isActive IS NULL OR u.isActive = :isActive)
        ORDER BY u.createdAt DESC
    """)
    fun searchUsers(
        @Param("query") query: String?,
        @Param("role") role: Role?,
        @Param("isActive") isActive: Boolean?,
        pageable: Pageable
    ): Page<UserEntity>
    
    @Query("""
        SELECT COUNT(u) FROM UserEntity u 
        WHERE (:query IS NULL OR 
               LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR 
               LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:role IS NULL OR u.role = :role)
        AND (:isActive IS NULL OR u.isActive = :isActive)
    """)
    fun countSearchUsers(
        @Param("query") query: String?,
        @Param("role") role: Role?,
        @Param("isActive") isActive: Boolean?
    ): Long
    
    // Statistics queries
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isActive = true")
    fun countActiveUsers(): Long
    
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role = :role")
    fun countByRole(@Param("role") role: Role): Long
    
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role = :role AND u.isActive = true")
    fun countByRoleAndIsActive(@Param("role") role: Role): Long
} 