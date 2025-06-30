# Development Setup Guide

## Prerequisites
- Docker and Docker Compose
- Java 21
- Gradle

## Quick Start

### 1. Start PostgreSQL Database
```bash
# Start the development PostgreSQL database
docker-compose -f docker-compose.dev.yml up postgres-dev -d

# Check if PostgreSQL is ready (should show "database system is ready to accept connections")
docker logs studymate-postgres-dev --tail 5
```

### 2. Start the Spring Boot Server
```bash
cd server
./gradlew bootRun
```

### 3. Test the API
```bash
# Test if the server is responding
curl http://localhost:8080/api/documents
# Should return: {"documents":[]}
```

## Database Configuration
- **Host**: localhost:5432
- **Database**: studymate
- **Username**: studymate_user
- **Password**: studymate_password

## API Endpoints
- `GET /api/documents` - List all documents
- `POST /api/documents/upload` - Upload a document
- `GET /api/documents/{id}/status` - Get document status
- `GET /api/documents/{id}/content` - Get processed content
- `GET /api/documents/{id}/download` - Download original file
- `PUT /api/documents/{id}/content` - Update document content
- `DELETE /api/documents/{id}` - Delete document

## Stopping Services
```bash
# Stop the server
# Ctrl+C in the terminal where gradlew bootRun is running

# Stop PostgreSQL
docker-compose -f docker-compose.dev.yml down
```

## Troubleshooting

### Port 5432 already in use
If you get a "port already allocated" error:
```bash
# Check what's using the port
docker ps | grep postgres

# Stop conflicting containers
docker stop <container-name>
```

### Database connection issues
- Ensure PostgreSQL container is running: `docker ps | grep postgres-dev`
- Check PostgreSQL logs: `docker logs studymate-postgres-dev`
- Verify database credentials in `server/src/main/resources/application.properties` 