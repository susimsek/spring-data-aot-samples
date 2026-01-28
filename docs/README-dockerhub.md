## Note App (Spring Boot 4 + Native)

Note CRUD sample application built with Spring Boot 4, Spring Data JPA (Hibernate), Envers and Liquibase.  
This image runs the app as a GraalVM native executable for fast startup and low memory usage.

This image runs a REST API for managing notes (CRUD) with soft delete (trash) and revision history. The application listens on the standard HTTP port `8080`. When running containers on the same Docker network, connect to `http://<container-name>:8080/` (same as connecting to a remote host).

### Features

- Note CRUD (create/list/get/update)
- Soft delete (trash) + permanent delete + restore
- Revision history (Hibernate Envers) + restore to a revision
- Tags, pinned, color; search (`q`) and pagination/sorting
- JWT-based authentication (`/api/auth/*`)
- Next.js UI (static export) served by Spring Boot (SPA routing)
- Actuator + Prometheus metrics + OpenTelemetry (OTLP) configuration

### How to use this image

#### 1) Start a PostgreSQL server

```bash
docker run --name postgresql --rm -d \
  -e POSTGRES_USER=note \
  -e POSTGRES_PASSWORD=note \
  -e POSTGRES_DB=note \
  -p 127.0.0.1:5432:5432 \
  postgres:18-alpine
```

#### 2) Generate a JWT secret

Generate at least a 256-bit secret:

```bash
openssl rand -base64 32
```

#### 3) Start the application

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/note \
  -e SPRING_DATASOURCE_USERNAME=note \
  -e SPRING_DATASOURCE_PASSWORD=note \
  -e SECURITY_JWT_SECRET="<paste-openssl-output>" \
  suayb/note-app:latest-native
```

Then you can open the web UI at `http://localhost:8080/` (or `http://<host-ip>:8080/`).

The REST API is available under `/api/**` (for example: `/api/notes`, `/api/auth/*`).

### Environment variables

| Name                         | Default | Description                                              |
| ---------------------------- | ------- | -------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`     | `prod`  | Active profile                                           |
| `SPRING_DATASOURCE_URL`      | (none)  | JDBC URL (e.g. `jdbc:postgresql://host:5432/note`)       |
| `SPRING_DATASOURCE_USERNAME` | (none)  | Database username                                        |
| `SPRING_DATASOURCE_PASSWORD` | (none)  | Database password                                        |
| `SECURITY_JWT_SECRET`        | (none)  | JWT secret (min 256-bit, e.g. `openssl rand -base64 32`) |
| `SPRING_LIQUIBASE_ENABLED`   | `true`  | Enable/disable Liquibase migrations (`false` to disable) |

### Health / Metrics

- Health: `GET /actuator/health`
- Prometheus: `GET /actuator/prometheus`

### Notes

- Liquibase migrations are enabled by default. Disable with `SPRING_LIQUIBASE_ENABLED=false` if you manage migrations separately.
- The Next.js frontend is bundled into the image as a static export and served from the same port (8080). Unknown non-API routes are handled as SPA routes.
