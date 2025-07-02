#!/bin/bash
# Start script for Render.com deployment

echo "Starting Spring Boot application..."

# Set the active profile to production
export SPRING_PROFILES_ACTIVE=prod

# Start the application with the built JAR file
java -jar target/myApp-0.0.1-SNAPSHOT.jar
