ARG MAVEN_IMAGE_NAME=maven
ARG MAVEN_IMAGE_VERSION=3.9-eclipse-temurin-21
ARG JAVA_RUNTIME_IMAGE_NAME=eclipse-temurin
ARG JAVA_RUNTIME_IMAGE_VERSION=21-jre

FROM ${MAVEN_IMAGE_NAME}:${MAVEN_IMAGE_VERSION} AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM ${JAVA_RUNTIME_IMAGE_NAME}:${JAVA_RUNTIME_IMAGE_VERSION}

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]