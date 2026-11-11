
ARG MAVEN_IMAGE_NAME=maven
ARG MAVEN_IMAGE_VERSION=3.9-eclipse-temurin-21

ARG JAVA_RUNTIME_IMAGE_NAME=eclipse-temurin
ARG JAVA_RUNTIME_IMAGE_VERSION=21-jre

# Build stage
FROM ${MAVEN_IMAGE_NAME}:${MAVEN_IMAGE_VERSION} AS build

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM ${JAVA_RUNTIME_IMAGE_NAME}:${JAVA_RUNTIME_IMAGE_VERSION}

WORKDIR /app

# Create non-root user
RUN groupadd --system spring \
    && useradd --system --gid spring spring

# Copy executable Spring Boot JAR
COPY --from=build /build/target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
