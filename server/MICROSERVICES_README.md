# Microservices Architecture Migration

This document describes the migration from the monolithic server to a microservices architecture using Traefik as the API Gateway.

## Architecture Overview

The application has been split into the following microservices:

### 1. Authentication Service 🔐
- **Port**: 8083
- **Database**: `auth_db`
- **Responsibilities**:
  - User registration and login
  - JWT token generation and validation
  - User profile management
  - Password management
- **Endpoints**:
  - `POST /auth/register` - User registration
  - `POST /auth/login` - User login
  - `POST /auth/refresh` - Token refresh
  - `GET /auth/me` - Get current user
  - `PUT /auth/me` - Update user profile
  - `POST /auth/me/password` - Change password

### 2. Document Management Service 📄
- **Port**: 8084
- **Database**: `document_db`
- **Responsibilities**:
  - Document upload and storage
  - Document metadata management
  - File operations (download, list)
  - Document status tracking
- **Endpoints**:
  - `POST /documents/upload` - Upload documents
  - `GET /documents` - List user documents
  - `GET /documents/{id}/status` - Get document status
  - `GET /documents/{id}/content` - Get document content
  - `GET /documents/{id}/download` - Download document
  - `PUT /documents/{id}/content` - Update document content

### 3. GenAI Gateway Service 🤖
- **Port**: 8085
- **Database**: None (stateless)
- **Responsibilities**:
  - Orchestrate GenAI requests
  - Handle business logic
  - Rate limiting
  - Communication with GenAI backend
- **Endpoints**:
  - `POST /genai/process` - Process document with AI
  - `GET /genai/status/{requestId}` - Get processing status
  - `POST /genai/chat` - Chat with AI
  - `GET /genai/health` - Health check

### 4. Eureka Service Discovery 🎯
- **Port**: 8761
- **Responsibilities**:
  - Service registration and discovery
  - Load balancing
  - Health monitoring
- **Dashboard**: `http://localhost:8761`

### 5. Traefik API Gateway 🌐
- **Ports**: 80 (HTTP), 443 (HTTPS), 8080 (Dashboard)
- **Responsibilities**:
  - Request routing
  - Load balancing
  - SSL termination
  - Rate limiting
  - Authentication validation
- **Dashboard**: `http://localhost:8080`

## Service Communication

### Internal Communication
- Services communicate via HTTP/REST APIs
- Service discovery through Eureka
- JWT tokens for authentication between services

### External Communication
- All external requests go through Traefik
- Traefik routes requests based on path prefixes:
  - `/auth/*` → Authentication Service
  - `/documents/*` → Document Service
  - `/genai/*` → GenAI Gateway Service
  - `/` → Client (React App)

## Database Architecture

### Separate Databases
- **auth_db**: User authentication and profile data
- **document_db**: Document storage and metadata
- **mydb**: Legacy database (for backward compatibility)

### Data Isolation
- Each service has its own database
- No direct database sharing between services
- Data consistency through service APIs

## Security

### JWT Authentication
- Centralized authentication in Auth Service
- JWT tokens contain user information
- Token validation in each service
- User ID extraction from tokens for authorization

### Service-to-Service Security
- Internal service communication via HTTP
- JWT token validation for service calls
- User context propagation via headers

## Deployment

### Docker Compose
```bash
# Start microservices
docker-compose -f docker-compose.microservices.yml up -d

# View logs
docker-compose -f docker-compose.microservices.yml logs -f

# Stop services
docker-compose -f docker-compose.microservices.yml down
```

### Individual Service Building
```bash
# Build auth service
cd server/auth-service
./gradlew build
docker build -t auth-service .

# Build document service
cd server/document-service
./gradlew build
docker build -t document-service .

# Build GenAI service
cd server/genai-service
./gradlew build
docker build -t genai-service .

# Build Eureka server
cd server/eureka-server
./gradlew build
docker build -t eureka-server .
```

## API Endpoints

### Authentication Service
```
POST /auth/register
POST /auth/login
POST /auth/refresh
GET  /auth/me
PUT  /auth/me
POST /auth/me/password
GET  /auth/users/{id}
GET  /auth/stats
```

### Document Service
```
POST   /documents/upload
GET    /documents
GET    /documents/{id}/status
GET    /documents/{id}/content
GET    /documents/{id}/download
PUT    /documents/{id}/content
```

### GenAI Service
```
POST /genai/process
GET  /genai/status/{requestId}
POST /genai/chat
GET  /genai/health
```

## Monitoring and Health Checks

### Service Health
- Each service exposes `/actuator/health` endpoint
- Eureka monitors service health
- Traefik dashboard shows service status

### Logging
- Centralized logging through Docker
- Service-specific log levels configurable
- Structured logging for better debugging

## Migration Strategy

### Phase 1: Service Extraction
1. Extract authentication logic to Auth Service
2. Extract document management to Document Service
3. Create GenAI Gateway Service
4. Set up Eureka Service Discovery

### Phase 2: API Gateway
1. Configure Traefik routing
2. Implement JWT validation
3. Set up load balancing
4. Configure SSL/TLS

### Phase 3: Database Separation
1. Create separate databases
2. Migrate data
3. Update service configurations
4. Test data consistency

### Phase 4: Client Updates
1. Update client API calls
2. Implement new authentication flow
3. Test all functionality
4. Deploy to production

## Benefits of Microservices

### Scalability
- Independent scaling of services
- Load balancing across instances
- Resource optimization

### Maintainability
- Smaller, focused codebases
- Independent deployments
- Technology flexibility

### Reliability
- Service isolation
- Fault tolerance
- Health monitoring

### Development
- Team autonomy
- Parallel development
- Technology diversity

## Troubleshooting

### Common Issues

1. **Service Discovery Issues**
   - Check Eureka server status
   - Verify service registration
   - Check network connectivity

2. **Authentication Issues**
   - Verify JWT token validity
   - Check JWT secret configuration
   - Validate user permissions

3. **Database Connection Issues**
   - Check database availability
   - Verify connection strings
   - Check database permissions

4. **Traefik Routing Issues**
   - Check service labels
   - Verify port configurations
   - Check Traefik dashboard

### Debug Commands
```bash
# Check service status
docker-compose -f docker-compose.microservices.yml ps

# View service logs
docker-compose -f docker-compose.microservices.yml logs [service-name]

# Check Eureka registry
curl http://localhost:8761/eureka/apps

# Test service endpoints
curl http://localhost:8083/auth/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/genai/health
```

## Future Enhancements

### Planned Improvements
1. **Circuit Breakers**: Implement resilience patterns
2. **Distributed Tracing**: Add observability
3. **Message Queues**: Implement async communication
4. **Caching**: Add Redis for performance
5. **Monitoring**: Implement comprehensive monitoring
6. **CI/CD**: Automated deployment pipelines

### Technology Stack Evolution
- Consider gRPC for service communication
- Implement GraphQL for flexible APIs
- Add Kubernetes for orchestration
- Implement service mesh (Istio/Linkerd) 