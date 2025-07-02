#!/bin/bash
# Build script for Render.com deployment

echo "Starting build process..."

# Install Maven dependencies and build the application
./mvnw clean package -DskipTests

echo "Build completed successfully!"
