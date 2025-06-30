package de.tum.cit.aet.server.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Value("\${app.cors.allowed-origins:http://localhost:3000}")
    private lateinit var allowedOrigins: String

    @Bean
    @Profile("!prod")
    fun developmentCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // Development: Allow localhost with different ports
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",
            "http://127.0.0.1:*"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L // Cache preflight for 1 hour
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    @Profile("prod")
    fun productionCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        // Production: Only allow specific trusted domains
        configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        
        // Only allow necessary HTTP methods
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
        
        // Only allow specific headers (no wildcards)
        configuration.allowedHeaders = listOf(
            "Content-Type",
            "Authorization", 
            "X-Requested-With",
            "Accept",
            "Cache-Control"
        )
        
        // Specify exactly which headers can be exposed to the client
        configuration.exposedHeaders = listOf(
            "Content-Disposition",
            "Content-Length",
            "Content-Type"
        )
        
        configuration.allowCredentials = true
        configuration.maxAge = 86400L // Cache preflight for 24 hours
        
        val source = UrlBasedCorsConfigurationSource()
        
        // Different CORS rules for different endpoints
        source.registerCorsConfiguration("/api/documents/**", configuration)
        
        // More restrictive for sensitive endpoints (if any)
        val restrictiveConfig = CorsConfiguration()
        restrictiveConfig.allowedOrigins = listOf("https://yourdomain.com") // Only main domain
        restrictiveConfig.allowedMethods = listOf("GET", "POST")
        restrictiveConfig.allowedHeaders = listOf("Content-Type", "Authorization")
        restrictiveConfig.allowCredentials = true
        
        // Example: Apply restrictive config to admin endpoints
        // source.registerCorsConfiguration("/api/admin/**", restrictiveConfig)
        
        return source
    }
} 