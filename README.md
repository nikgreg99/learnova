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

## Database Migrations (Flyway)

Learnova uses **Flyway** for automated, version-controlled database schema migrations across development, testing, and production environments.

Hibernate is intentionally configured with `ddl-auto: validate` so that Flyway is the single source of truth for the schema, while Hibernate ensures application entity mappings stay strictly synchronized.

### Migration Script Naming Convention

Migration scripts are stored in:
`backend/src/main/resources/db/migration`

Scripts must strictly follow the Flyway versioning pattern:

```text
V<Version>__<Description>.sql
```

- **`V`**: Prefix denoting a versioned migration.
- **`<Version>`**: Numeric version (e.g. `1`, `2`, `2.1`). Flyway applies migrations in ascending version order.
- **`__`**: Exactly **two underscores** as the separator.
- **`<Description>`**: Brief description in lowercase with words separated by underscores (e.g., `create_course_table`).
- **`.sql`**: Standard SQL file extension.

*Example:* `V1__create_course_table.sql`

### How to Add a New Migration

1. Create a new `.sql` file in `backend/src/main/resources/db/migration/` using the next version number:

   ```bash
   touch backend/src/main/resources/db/migration/V2__add_instructor_column.sql
   ```

2. Write standard PostgreSQL DDL statements:

   ```sql
   ALTER TABLE course ADD COLUMN instructor VARCHAR(255);
   ```

3. Update the corresponding JPA entity class if applicable.
4. Run `./mvnw test` to verify that Flyway executes the migration against the test database and Hibernate validation succeeds.

### Tracking and Failure Handling

- **Execution Tracking:** Flyway maintains a `flyway_schema_history` table in PostgreSQL. It records the version, description, execution timestamp, and a **checksum** of each applied script. Previously applied migrations are never re-run.
- **Checksum Integrity:** Never alter an existing migration script after it has been merged or applied. Flyway compares checksums on application startup and will fail to boot if a mismatch is detected.
- **Failure Handling:** If a migration script contains invalid SQL or fails during execution, Spring Boot immediately halts context initialization. The database will not be left in an unversioned state, and the application will not serve traffic until the migration error is resolved.

---

## Testing

Run the full test suite (including Testcontainers integration tests):

```bash
cd backend
./mvnw test
```
