# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Step 1: Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Step 2: Give read/execute permissions to the app directory and jar file
# Isse koi bhi error nahi aayega chahe koi bhi user ho
RUN chmod 777 /app && chmod 777 app.jar

# Step 3: Switch to UID 1000 (Hugging Face Requirement) without creating it
USER 1000

EXPOSE 7860

# Step 4: Run the application
ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
