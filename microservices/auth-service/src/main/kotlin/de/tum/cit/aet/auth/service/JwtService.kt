package de.tum.cit.aet.auth.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService {
    
    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String
    
    @Value("\${jwt.expiration}")
    private var jwtExpiration: Long = 86400000 // 24 hours
    
    @Value("\${jwt.refresh-expiration}")
    private var refreshExpiration: Long = 604800000 // 7 days
    
    private val signatureAlgorithm = SignatureAlgorithm.HS256
    
    private fun getSigningKey(): Key {
        val keyBytes = jwtSecret.toByteArray()
        return SecretKeySpec(keyBytes, signatureAlgorithm.jcaName)
    }
    
    fun generateToken(userDetails: UserDetails): String {
        return generateToken(emptyMap(), userDetails)
    }
    
    fun generateToken(extraClaims: Map<String, Any>, userDetails: UserDetails): String {
        return buildToken(extraClaims, userDetails, jwtExpiration)
    }
    
    fun generateRefreshToken(userDetails: UserDetails): String {
        return buildToken(emptyMap(), userDetails, refreshExpiration)
    }
    
    private fun buildToken(
        extraClaims: Map<String, Any>,
        userDetails: UserDetails,
        expiration: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)
        
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), signatureAlgorithm)
            .compact()
    }
    
    fun extractUsername(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }
    
    fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }
    
    fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }
    
    private fun extractAllClaims(token: String): Claims {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            logger.error("Error extracting claims from token: ${e.message}")
            throw SecurityException("Invalid token")
        }
    }
    
    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        return try {
            val username = extractUsername(token)
            username == userDetails.username && !isTokenExpired(token)
        } catch (e: Exception) {
            logger.error("Token validation failed: ${e.message}")
            false
        }
    }
    
    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }
    
    fun getExpirationTime(): Long {
        return jwtExpiration
    }
    
    fun getRefreshExpirationTime(): Long {
        return refreshExpiration
    }
    
    fun getRemainingTime(token: String): Long {
        val expiration = extractExpiration(token)
        val now = Date()
        return if (expiration.after(now)) {
            expiration.time - now.time
        } else {
            0L
        }
    }
    
    fun isRefreshTokenValid(token: String, userDetails: UserDetails): Boolean {
        return try {
            val username = extractUsername(token)
            username == userDetails.username && !isTokenExpired(token)
        } catch (e: Exception) {
            logger.error("Refresh token validation failed: ${e.message}")
            false
        }
    }
    
    // Utility methods for token information
    fun getTokenInfo(token: String): Map<String, Any> {
        return try {
            val claims = extractAllClaims(token)
            mapOf(
                "username" to claims.subject,
                "issuedAt" to claims.issuedAt,
                "expiration" to claims.expiration,
                "remainingTime" to getRemainingTime(token)
            )
        } catch (e: Exception) {
            logger.error("Error getting token info: ${e.message}")
            emptyMap()
        }
    }
    
    fun extractTokenFromHeader(authHeader: String?): String? {
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            null
        }
    }
} 