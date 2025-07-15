package de.tum.cit.aet.shared.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.Key
import javax.crypto.spec.SecretKeySpec

@Component
class JwtAuthenticationFilter(
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractTokenFromRequest(request)
            if (token != null && validateToken(token)) {
                val username = extractUsername(token)
                val userDetails = userDetailsService.loadUserByUsername(username)
                
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.authorities
                )
                
                SecurityContextHolder.getContext().authentication = authentication
                
                // Add user ID to request headers for microservice communication
                val userId = extractUserIdFromToken(token)
                if (userId != null) {
                    // Create a custom request wrapper to add headers
                    val wrappedRequest = object : HttpServletRequestWrapper(request) {
                        override fun getHeader(name: String): String? {
                            return if (name == "X-User-ID") userId else super.getHeader(name)
                        }
                    }
                    filterChain.doFilter(wrappedRequest, response)
                    return
                }
            }
        } catch (e: Exception) {
            logger.error("JWT authentication error: {}", e.message)
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            null
        }
    }
    
    private fun validateToken(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            !isTokenExpired(claims)
        } catch (e: Exception) {
            logger.error("Token validation failed: {}", e.message)
            false
        }
    }
    
    private fun extractUsername(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }
    
    private fun extractUserIdFromToken(token: String): String? {
        return try {
            val claims = extractAllClaims(token)
            claims["userId"] as? String
        } catch (e: Exception) {
            logger.error("Error extracting user ID from token: {}", e.message)
            null
        }
    }
    
    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
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
        return SecretKeySpec(keyBytes, "HmacSHA256")
    }
}

// Simple wrapper to add custom headers
class HttpServletRequestWrapper(private val request: HttpServletRequest) : HttpServletRequest by request {
    private val customHeaders = mutableMapOf<String, String>()
    
    fun addHeader(name: String, value: String) {
        customHeaders[name] = value
    }
    
    override fun getHeader(name: String): String? {
        return customHeaders[name] ?: request.getHeader(name)
    }
} 