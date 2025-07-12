-- Database setup script for StudyMate microservices
-- This script creates the three separate databases needed for the microservices architecture

-- Create the three databases
CREATE DATABASE studymate_auth;
CREATE DATABASE studymate_documents;
CREATE DATABASE studymate_ai;

-- Grant permissions to postgres user
GRANT ALL PRIVILEGES ON DATABASE studymate_auth TO postgres;
GRANT ALL PRIVILEGES ON DATABASE studymate_documents TO postgres;
GRANT ALL PRIVILEGES ON DATABASE studymate_ai TO postgres;

-- Connect to auth database and create extensions if needed
\c studymate_auth;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Connect to documents database and create extensions if needed
\c studymate_documents;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Connect to ai database and create extensions if needed
\c studymate_ai;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Switch back to default database
\c postgres; 