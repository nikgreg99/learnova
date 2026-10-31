# Learnova

Learnova is a modern e-learning management platform built with Spring Boot 4, Java 21, and PostgreSQL.

---

## API Documentation

Learnova uses OpenAPI 3.1 and Swagger UI for automated and interactive REST API documentation.

When the application is running, you can access the documentation at:

| Resource | URL | Description |
| :--- | :--- | :--- |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Interactive web interface to explore and test endpoints |
| **OpenAPI Spec (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Raw OpenAPI 3.1 JSON contract |

### Interactive Testing with Swagger UI
1. Navigate to `http://localhost:8080/swagger-ui.html` in your browser.
2. Expand the **Courses** tag to view all available course management endpoints (`GET`, `POST`, `PUT`, `DELETE`).
3. Click on any endpoint, select **Try it out**, fill in parameters or the sample payload, and click **Execute** to send requests to the API.
4. Inspect the expected responses and HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`).

### Environment Configuration
Swagger UI and OpenAPI endpoints are enabled by default for development. To disable them in production environments, set the following properties in `application.yaml` or via environment variables:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

---

## Getting Started

### Prerequisites
- Java 21 (Eclipse Temurin recommended)
- Docker and Docker Compose

### 1. Environment Setup
Create a `.env` file in the project root:
```bash
cp .env.example .env
```

### 2. Run with Docker Compose
To build and start both the backend and PostgreSQL database:
```bash
docker compose up -d --build
```
The API will be available at `http://localhost:8080`.

To view application logs:
```bash
docker compose logs -f learnova-backend
```

### 3. Run Locally for Development
1. Start PostgreSQL:
   ```bash
   docker compose up -d postgres
   ```
2. Start the Spring Boot backend:
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

---

## Testing

Run the full test suite (including Testcontainers integration tests):
```bash
cd backend
./mvnw test
```
