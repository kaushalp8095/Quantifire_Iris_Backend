# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Sab kuch copy karo
COPY . .

# 2. IMPORTANT: Build se pehle manual install command chalao
# Ye command common.jar ko Maven ki internal repo mein daal degi
RUN mvn install:install-file \
    -Dfile=libs/common.jar \
    -DgroupId=com.project \
    -DartifactId=common \
    -Dversion=0.0.1-SNAPSHOT \
    -Dpackaging=jar

# 3. Ab tumhara main build chalega (Ab usey common.jar mil jayegi)
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Step 1: Copy the jar file
COPY --from=build /app/target/*.jar app.jar

# Step 2: Permissions
RUN chmod 777 /app && chmod 777 app.jar

# Step 3: User
USER 1000

EXPOSE 7860

ENTRYPOINT ["java","-jar","app.jar","--server.port=7860","--server.address=0.0.0.0"]
