#--------------
# Build Stage
#--------------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

#Copy Maven files first (better caching)
COPY pom.xml ./
COPY src ./src

#Build the jar (skip tests for faster build)
RUN mvn clean package -DskipTests

#-------------
# Runtime stage
#-------------

FROM eclipse-temurin:21-jdk-alpine AS runner

WORKDIR /app

#Non-root user for security

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

#Copy the jar from the builder stage

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

#Run the Spring Boot app
ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]