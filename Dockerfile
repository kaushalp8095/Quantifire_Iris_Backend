# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Step 1: Hugging Face Security Policy - Create non-root user with UID 1000
RUN useradd -m -u 1000 user
USER user
ENV HOME=/home/user \
    PATH=/home/user/.local/bin:$PATH

# Step 2: Copy jar with correct user permissions
COPY --from=build --chown=user:user /app/target/*.jar app.jar

EXPOSE 7860

# Step 3: Explicitly bind to 0.0.0.0 to fix "Refused to Connect"
ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
