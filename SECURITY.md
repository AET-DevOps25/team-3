# Security Configuration Guide

## CORS (Cross-Origin Resource Sharing) Configuration

### Development vs Production

Our application uses **profile-based CORS configuration** for enhanced security:

#### Development Profile (default)
```kotlin
@Profile("!prod") // Runs when NOT in production
fun developmentCorsConfigurationSource() {
    // Allows localhost with any port (http://localhost:*)
    // Permissive for development convenience
}
```

#### Production Profile
```kotlin
@Profile("prod") // Only runs in production
fun productionCorsConfigurationSource() {
    // Strict origin whitelist
    // Limited headers and methods
    // Enhanced security
}
```

### Security Improvements

#### 1. **Environment-Based Origins**
```properties
# Development
app.cors.allowed-origins=http://localhost:3000

# Production
app.cors.allowed-origins=https://studymate.yourdomain.com,https://app.yourdomain.com
```

#### 2. **Method Restrictions**
```kotlin
// Development: All methods including OPTIONS for debugging
configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")

// Production: Only necessary methods
configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
```

#### 3. **Header Whitelisting**
```kotlin
// Development: Allow all headers (*)
configuration.allowedHeaders = listOf("*")

// Production: Specific headers only
configuration.allowedHeaders = listOf(
    "Content-Type",      // For JSON/file uploads
    "Authorization",     // For authentication
    "X-Requested-With",  // For AJAX identification
    "Accept",           // Content negotiation
    "Cache-Control"     // Caching directives
)
```

#### 4. **Endpoint-Specific CORS**
```kotlin
// General API endpoints
source.registerCorsConfiguration("/api/documents/**", configuration)

// More restrictive for sensitive endpoints
source.registerCorsConfiguration("/api/admin/**", restrictiveConfig)
```

#### 5. **Response Header Control**
```kotlin
// Only expose necessary headers to frontend
configuration.exposedHeaders = listOf(
    "Content-Disposition",  // For file downloads
    "Content-Length",       // File size info
    "Content-Type"         // MIME type info
)
```

### Running in Different Environments

#### Development (default)
```bash
./gradlew bootRun
# Uses permissive CORS for localhost development
```

#### Production
```bash
# Set production profile
export SPRING_PROFILES_ACTIVE=prod
export ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com

./gradlew bootRun
# Uses strict CORS configuration
```

#### Docker Production
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
ENV ALLOWED_ORIGINS=https://yourdomain.com
```

### Security Best Practices

#### ✅ **DO**
- Use specific domain names in production
- Limit allowed methods to what you actually need
- Whitelist specific headers only
- Use HTTPS in production origins
- Set reasonable cache times (`maxAge`)
- Use environment variables for domains

#### ❌ **DON'T**
- Use `*` wildcards in production
- Allow `allowCredentials=true` with wildcard origins
- Expose sensitive headers
- Use HTTP origins in production
- Hardcode production domains in code

### Example Production Deployment

```yaml
# docker-compose.prod.yml
services:
  studymate-server:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ALLOWED_ORIGINS=https://studymate.com,https://app.studymate.com
      - DATABASE_URL=jdbc:postgresql://prod-db:5432/studymate
```

### Testing CORS Configuration

#### Development Test
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     http://localhost:8080/api/documents/upload
```

#### Production Test
```bash
curl -H "Origin: https://studymate.com" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     https://api.studymate.com/api/documents/upload
```

### Troubleshooting CORS Issues

1. **Check Active Profile**
   ```bash
   # Look for: "The following profiles are active: prod"
   curl http://localhost:8080/actuator/env
   ```

2. **Verify Allowed Origins**
   ```bash
   # Check if your domain is in the allowed list
   echo $ALLOWED_ORIGINS
   ```

3. **Browser Network Tab**
   - Look for `Access-Control-Allow-Origin` header in response
   - Check if preflight OPTIONS request succeeds

### Additional Security Considerations

- **Content Security Policy (CSP)**: Add CSP headers for XSS protection
- **Rate Limiting**: Implement rate limiting for upload endpoints
- **Authentication**: Add JWT/OAuth for protected endpoints
- **Input Validation**: Validate file types and sizes
- **Audit Logging**: Log all API access for security monitoring 