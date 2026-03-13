# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Pehle Common module copy karein aur install karein
COPY common/ /app/common/
RUN cd /app/common && mvn clean install -DskipTests

# 2. Ab Agency (Client) module copy karein
COPY client/ /app/client/
# Agar aapka pom.xml root mein hai toh uske hisab se adjust karein

# 3. Agency ko build karein (Ab ise common mil jayega)
RUN cd /app/client && mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/client/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]