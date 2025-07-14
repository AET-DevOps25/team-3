package de.tum.cit.aet.document.config

import de.tum.cit.aet.document.service.DocumentUserDetailsService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.Key
import javax.crypto.spec.SecretKeySpec

@Component
class JwtAuthenticationFilter(
    private val userDetailsService: DocumentUserDetailsService
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.info("JwtAuthenticationFilter: Received request for URI: ${request.requestURI}")
        
        // Skip JWT authentication for internal endpoints
        if (request.requestURI.contains("/api/documents/internal/")) {
            logger.info("JwtAuthenticationFilter: Skipping JWT authentication for internal endpoint: ${request.requestURI}")
            val headers = request.headerNames.toList().joinToString(", ") { "$it: ${request.getHeader(it)}" }
            logger.info("JwtAuthenticationFilter: Request headers: $headers")
            
            // Set anonymous authentication
            SecurityContextHolder.getContext().authentication = AnonymousAuthenticationToken(
                "key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            )
            filterChain.doFilter(request, response)
            return
        }
        
        logger.info("JwtAuthenticationFilter: Processing JWT authentication for URI: ${request.requestURI}")
        try {
            val token = extractTokenFromRequest(request)
            if (token != null && validateToken(token)) {
                val username = extractUsername(token)
                val userDetails = userDetailsService.loadUserByUsername(username)
                
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.authorities
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                
                SecurityContextHolder.getContext().authentication = authentication
                
                // Add user ID to request attributes for microservice communication
                val userId = extractUserIdFromToken(token)
                if (userId != null) {
                    request.setAttribute("X-User-ID", userId)
                }
                logger.info("JwtAuthenticationFilter: Successfully authenticated user: $username")
            }
        } catch (e: Exception) {
            logger.error("JWT authentication error: ${e.message}")
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }
    
    private fun validateToken(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            !isTokenExpired(claims)
        } catch (e: Exception) {
            logger.error("Token validation failed: ${e.message}")
            false
        }
    }
    
    private fun extractUsername(token: String): String {
        return extractAllClaims(token).subject
    }
    
    private fun extractUserIdFromToken(token: String): String? {
        return try {
            val claims = extractAllClaims(token)
            claims["userId"] as? String ?: claims.subject
        } catch (e: Exception) {
            logger.error("Error extracting user ID from token: ${e.message}")
            null
        }
    }
    
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body
    }
    
    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(java.util.Date())
    }
    
    private fun getSigningKey(): Key {
        val keyBytes = jwtSecret.toByteArray()
        return SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.jcaName)
    }
} 