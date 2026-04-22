# Use Java 21 base image
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/*.jar app.jar

# Expose port (HF uses 7860 internally)
EXPOSE 7860

# Run app on port 7860
ENTRYPOINT ["java","-jar","/app/app.jar","--server.port=7860"]
