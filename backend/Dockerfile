# Multi-stage build for backend root directory
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and source code (including static frontend resources)
COPY pom.xml .
COPY src ./src

# Build JAR package
RUN mvn clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/clinic-website-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
