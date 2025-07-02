FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the JAR file
COPY target/myApp-0.0.1-SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
