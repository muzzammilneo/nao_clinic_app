# Multi-stage build for repository root
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy backend source
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src

# Build JAR package
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/target/clinic-website-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
