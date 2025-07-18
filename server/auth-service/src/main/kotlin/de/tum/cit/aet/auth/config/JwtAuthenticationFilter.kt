package de.tum.cit.aet.auth.config

import de.tum.cit.aet.auth.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("JwtAuthenticationFilter: Processing request for URI: ${request.requestURI}")
        
        // Skip JWT authentication for public endpoints
        if (isPublicEndpoint(request.requestURI)) {
            logger.debug("JwtAuthenticationFilter: Skipping JWT authentication for public endpoint: ${request.requestURI}")
            filterChain.doFilter(request, response)
            return
        }
        
        try {
            val token = extractTokenFromRequest(request)
            if (token != null && SecurityContextHolder.getContext().authentication == null) {
                val username = jwtService.extractUsername(token)
                val userDetails = userDetailsService.loadUserByUsername(username)
                
                if (jwtService.isTokenValid(token, userDetails)) {
                    val authentication = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                    
                    logger.debug("JwtAuthenticationFilter: Successfully authenticated user: $username")
                }
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
    
    private fun isPublicEndpoint(uri: String): Boolean {
        val publicEndpoints = listOf(
            "/api/auth/register",
            "/api/auth/login", 
            "/api/auth/refresh",
            "/actuator"
        )
        return publicEndpoints.any { uri.startsWith(it) }
    }
} 