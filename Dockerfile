# Multi-stage Docker build for easy deployment on Render / Railway / Cloud
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy source files
COPY backend /app/backend
COPY frontend /app/frontend

# Bundle frontend static files into backend resources so the JAR is self-contained
RUN cp -r /app/frontend/* /app/backend/src/main/resources/static/ 2>/dev/null || true

# Build production JAR
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/target/clinic-website-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
