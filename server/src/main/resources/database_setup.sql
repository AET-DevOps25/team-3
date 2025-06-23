-- Create database and user for StudyMate application
-- Run this as PostgreSQL superuser (postgres)

-- Create database
CREATE DATABASE studymate;

-- Create user
CREATE USER studymate_user WITH ENCRYPTED PASSWORD 'studymate_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE studymate TO studymate_user;

-- Connect to studymate database
\c studymate;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO studymate_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO studymate_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO studymate_user;

-- Note: The table will be created automatically by Hibernate when the application starts
-- due to spring.jpa.hibernate.ddl-auto=update in application.properties 