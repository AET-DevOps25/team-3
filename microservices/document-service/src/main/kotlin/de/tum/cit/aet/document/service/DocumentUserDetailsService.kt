package de.tum.cit.aet.document.service

import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class DocumentUserDetailsService : UserDetailsService {
    
    private val logger = LoggerFactory.getLogger(DocumentUserDetailsService::class.java)
    
    override fun loadUserByUsername(username: String): UserDetails {
        logger.info("Loading user details for username: {}", username)
        
        // Since we're in a microservice architecture, we don't have direct access to user database
        // The JWT token validation in the filter ensures the user is valid
        // We just need to create a UserDetails object for Spring Security
        
        return User.builder()
            .username(username)
            .password("") // Password is not needed since we validate via JWT
            .authorities(listOf(SimpleGrantedAuthority("ROLE_USER")))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
} 