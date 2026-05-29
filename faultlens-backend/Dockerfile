FROM maven:3.9.7-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy the entire project
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/api/target/*.jar app.jar

# Expose the new backend port
EXPOSE 3600

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
