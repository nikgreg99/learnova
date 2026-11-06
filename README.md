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
3. Use **Authorize** to provide a Keycloak access token with the HTTP bearer authentication scheme.
4. Click on any endpoint, select **Try it out**, fill in parameters or the sample payload, and click **Execute** to send requests to the API.
5. Inspect the expected responses and HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`).

### Testing with Postman

Import `postman/learnova.json`. The collection uses the `learnova-postman` Keycloak client and Authorization Code with PKCE (S256); its default URLs target the local API at `http://localhost:8080` and Keycloak at `http://localhost:8081`.

1. Start the Docker Compose services and open the collection's **Authorization** tab. OAuth 2.0 Authorization Code with PKCE (S256) is preconfigured for client `learnova-postman`, the Keycloak token endpoints, and Postman's callback URL. The public client requires no client secret.
2. Select **Get New Access Token**, sign in to Keycloak with the local test account `test-student` (password: `LearnovaLocal123!`, as defined in the imported development realm), then select **Use Token**. Postman stores the token for the collection and automatically adds it to protected requests; no manual token copying is needed.
3. **Create user** is public and intentionally omits `roles`, so the API assigns the default `STUDENT` role. User-management operations and course endpoints inherit the collection's access token.

The realm import configures the Postman client for Postman's desktop (`https://oauth.pstmn.io/v1/callback`) and web (`https://oauth.pstmn.io/v1/browser-callback`) callback URLs with PKCE. The backend service account has `manage-users` to provision and enable/disable identities, plus `query-users` and `view-users` to find and read test accounts. At backend startup, enabled synchronization (`KEYCLOAK_SYNC_TEST_USERS=true`) imports Keycloak users whose usernames start with `test-` into the application database; this includes the imported `test-student` account and other test accounts created in the realm. If Keycloak reports `invalid_redirect_uri`, make sure Postman's configured callback URL exactly matches one of the configured URLs. Keycloak imports realms on first initialization; an existing persistent Keycloak database may not pick up changes to the import file. In that case update the client and its service-account role in the Keycloak Admin Console, or recreate only disposable local Keycloak data after backing up anything you need.

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
- **Formatting Existing Migrations:** Formatting V1–V4 changes their checksums. Databases that already applied the previous versions must have their Flyway checksums reconciled through the deployment process before starting the updated application.
- **Failure Handling:** If a migration script contains invalid SQL or fails during execution, Spring Boot immediately halts context initialization. The database will not be left in an unversioned state, and the application will not serve traffic until the migration error is resolved.

---

## Testing

Run the full test suite (including Testcontainers integration tests):

```bash
cd backend
./mvnw test
```

## Continuous Integration and Container Image

The GitHub Actions workflow runs on pushes to `main` and version tags matching `v*`. It builds and tests the backend, runs PMD checks, then publishes the Docker image to `ghcr.io/<owner>/learnova`. PMD rules are defined in `backend/config/pmd/ruleset.xml`, and any violations fail the Maven `verify` phase.

Each published image has a `sha-<commit-sha>` tag; version-tag pushes also receive the matching release tag (for example, `v1.2.3`). The workflow verifies that the image can be pulled by digest and that the registry manifest matches the generated SHA-256 digest. The tags and digest are reported in the workflow run summary.

To allow unauthenticated pulls, change the GHCR package visibility to **Public** in the package settings. Private packages remain pullable by users and workflows with package read access.

---

## Code Style & Formatting (Spotless & EditorConfig)

Learnova enforces consistent code style and formatting across all machines and IDEs using **Spotless** and **EditorConfig**.

- **`.editorconfig`**: Synchronizes basic editor rules (indentation style, tab width, trailing whitespace, line endings) across IDEs such as VS Code and IntelliJ IDEA.
- **Spotless (`spotless-maven-plugin`)**: Enforces idiomatic Java formatting (Palantir Java Format with 4-space indentation, unused import removal, and clean record/annotation layout) and formats Flyway SQL migrations with the DBeaver formatter.

### Verify Formatting (CI Check)

To verify that all files comply with the formatting rules without modifying them:

```bash
cd backend
./mvnw spotless:check
```

If any files violate the style guidelines, this command will report the discrepancies and fail the build.

### Auto-Format Codebase

To automatically format Java source files and Flyway SQL migration scripts:

```bash
cd backend
./mvnw spotless:apply
```

### Git Pre-Commit Hook (Automatic Formatting on Commit)

A shared Git pre-commit hook is provided in `.githooks/` to automatically format staged Java files whenever you commit.

To activate the hook on your local machine, run once from the project root:

```bash
git config core.hooksPath .githooks
```
