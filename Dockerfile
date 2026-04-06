# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Pura project copy karein (Jisme libs folder aur pom.xml ho)
COPY . .

# 2. Jo naya common.jar aapne dala hai, use manually Maven repo me install karein
# Note: Agar aapki jar 'libs/common.jar' me hai toh niche wala path sahi hai
RUN mvn install:install-file \
    -Dfile=libs/common.jar \
    -DgroupId=com.project \
    -DartifactId=common \
    -Dversion=0.0.1-SNAPSHOT \
    -Dpackaging=jar

# 3. Ab Agency (Client) ko build karein
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]