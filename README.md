# Note App Sample (Spring Boot 4 + Native)

[![Build Status](https://circleci.com/gh/susimsek/spring-data-aot-samples/tree/main.svg?style=shield)](https://circleci.com/gh/susimsek/spring-data-aot-samples/tree/main)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=note-app&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=note-app)
[![Vulnerabilities](https://snyk.io/test/github/susimsek/spring-data-aot-samples/badge.svg)](https://snyk.io/test/github/susimsek/spring-data-aot-samples)
[![Docker Pulls](https://img.shields.io/docker/pulls/suayb/note-app?label=Docker%20Pulls)](https://hub.docker.com/r/suayb/note-app)
[![Docker Image Size](https://img.shields.io/docker/image-size/suayb/note-app/latest-native?label=Image%20Size)](https://hub.docker.com/r/suayb/note-app)
[![Java](https://img.shields.io/badge/Java-25%2B-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)](https://spring.io/projects/spring-boot)
[![GraalVM](https://img.shields.io/badge/GraalVM-25%2B-red)](https://www.graalvm.org/)

This repository is a “Note” sample application built with Spring Boot 4 + Spring Data JPA (Hibernate) + Envers + Liquibase. It can run on the JVM and can also be built as a GraalVM Native Image (native executable / native container).

## Features

- Note CRUD, soft delete (trash), permanent delete and restore
- Revision history (Hibernate Envers) and restore to a revision
- Tags, pin, color; search (`q`), pagination/sorting
- JWT-based auth (`/api/auth/*`) + cookie-based session handling
- Schema + seed data via Liquibase
- Actuator, Prometheus metrics and OpenTelemetry (OTLP) configuration

## Requirements

- Java: `25+` (enforced by Maven Enforcer)
- Maven Wrapper: `./mvnw`
- Optional:
  - Docker (for Jib, docker-compose, Helm)
  - GraalVM Native Image (for native builds)

## Project Layout

- Application code: `src/main/java`
- Test code: `src/test/java`
- Configuration: `src/main/resources/config`
- Frontend (Next.js): `src/main/webapp`
- Liquibase: `src/main/resources/config/liquibase`
- Docker compose files: `src/main/docker`
- Helm chart: `helm/note-app`
- Monitoring manifest: `helm/monitoring/lgtm.yaml`

## Configuration and Profiles

Configuration lives under `src/main/resources/config`:

- `application.yml` (shared)
- `application-dev.yml` (H2, swagger enabled, debug logs, etc.)
- `application-prod.yml` (PostgreSQL, swagger disabled, cache headers, etc.)

Maven profiles:

- `dev` (default)
- `prod`
- `native` (GraalVM native build + Jib native-image extension)
- `docker-compose` (Spring Boot docker-compose integration dependency)

Note: `spring.profiles.active` in `application.yml` is filled via Maven resource filtering (e.g. `dev`/`prod`).

## Run Locally

### Dev (H2)

```bash
./mvnw spring-boot:run
```

- UI: `http://localhost:8080/`
- Login: `http://localhost:8080/login`
- DB: `jdbc:h2:mem:note` (seeded by Liquibase)

### Frontend (Next.js)

Frontend sources live in `src/main/webapp`. For local frontend dev (Next dev server) against the Spring Boot backend:

```bash
cd src/main/webapp
npm ci
npm run dev
```

Next dev runs at `http://localhost:3000`. API calls use relative `/api/...` URLs and are proxied to the backend via dev rewrites in `next.config.js`.

### Prod (PostgreSQL)

Start PostgreSQL first:

```bash
docker compose -f src/main/docker/postgresql.yml up -d
```

Then run the app with the `prod` profile:

```bash
export SECURITY_JWT_SECRET="$(openssl rand -base64 32)"
export SPRING_DATASOURCE_USERNAME=note
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/note
./mvnw -Pprod spring-boot:run
```

## Swagger UI / OpenAPI

Swagger UI is enabled in `dev` and disabled in `prod` (see `application-dev.yml` / `application-prod.yml`).

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON (springdoc): `http://localhost:8080/v3/api-docs`

## API Quick Overview

Auth:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/change-password`

Notes:

- `POST /api/notes`
- `GET /api/notes` (search `q`, paging/sort)
- `GET /api/notes/{id}`
- `PUT /api/notes/{id}`
- `PATCH /api/notes/{id}`
- `GET /api/notes/deleted`
- `DELETE /api/notes/deleted` (empty trash)
- `DELETE /api/notes/{id}` (soft delete)
- `POST /api/notes/bulk`
- `POST /api/notes/{id}/restore`
- `DELETE /api/notes/{id}/permanent`
- `GET /api/notes/{id}/revisions`
- `GET /api/notes/{id}/revisions/{revisionId}`
- `POST /api/notes/{id}/revisions/{revisionId}/restore`

Sharing:

- `POST /api/notes/{id}/share`
- `GET /api/notes/{id}/share`
- `GET /api/notes/share`
- `DELETE /api/notes/share/{tokenId}`
- `GET /api/share/{token}`

Tags:

- `GET /api/tags/suggest` (query `q`, paging)

Admin:

- `GET /api/admin/users/search` (query `q`, paging)
- `POST /api/admin/notes`
- `GET /api/admin/notes` (search `q`, paging/sort)
- `GET /api/admin/notes/deleted`
- `DELETE /api/admin/notes/deleted` (empty trash)
- `POST /api/admin/notes/bulk`
- `POST /api/admin/notes/{id}/owner`
- `POST /api/admin/notes/{id}/share`
- `GET /api/admin/notes/{id}/share`
- `GET /api/admin/notes/share`
- `DELETE /api/admin/notes/share/{tokenId}`

## Build

- Unit tests: `./mvnw test`
- Integration tests: `./mvnw verify` (Failsafe `*IT*`)
- Full verification: `./mvnw verify` (includes Checkstyle + Spotless check + JaCoCo)
- Package: `./mvnw -DskipTests package`

JaCoCo reports:

- Unit: `target/site/jacoco/jacoco.xml`
- Integration: `target/site/jacoco-it/jacoco.xml`

## Performance Tests

Performance tests are done with Gatling and are located in the `src/test/java/gatling/simulations` folder.

Run all simulations:

```bash
./mvnw gatling:test
```

## Code Quality

### Checkstyle

Checkstyle runs automatically in the `validate` phase.

```bash
./mvnw -DskipTests checkstyle:check
```

Config: `checkstyle.xml` and `checkstyle-suppressions.xml`

### Spotless

Spotless runs `spotless:check` in the `compile` phase. To apply formatting:

```bash
./mvnw -DskipTests spotless:apply
```

### Sonar

If you use SonarCloud (or SonarQube) in your pipeline, you can run analysis locally as well.

```bash
export SONAR_TOKEN=...
./mvnw -B -ntp -Pprod verify sonar:sonar \
  -Dsonar.token="$SONAR_TOKEN"
```

## Docker Image

Build a JVM container image without a Dockerfile:

```bash
./mvnw -Pprod -DskipTests jib:dockerBuild
```

Push to a registry:

```bash
./mvnw -Pprod -DskipTests jib:build -Djib.to.image=YOUR_IMAGE
```

Defaults (from `pom.xml`):

- Base image: `eclipse-temurin:25-jre-alpine`
- Platform: `linux/arm64` (override with `-Djib-maven-plugin.architecture=amd64` if needed)

Native Docker image (GraalVM Native Image + Jib):

```bash
./mvnw -Pprod,native -DskipTests jib:dockerBuild \
  -Djib.to.image=note-app:latest-native
```

Defaults (from `pom.xml`, `native` profile):

- Base image: `scratch` (contains only the native binary; no JVM)
- Working directory: `/tmp`
- Platform: `linux/arm64` (override with `-Djib-maven-plugin.architecture=amd64` if needed)

## GraalVM Native Image

Native executable:

```bash
./mvnw -Pprod,native -DskipTests native:compile
```

Output: `target/native-executable`

Native-image build arguments:

```bash
./mvnw -ntp -Pprod,native -DskipTests \
  -DbuildArgs="--no-fallback,-Os,--static,--libc=musl,--verbose,-J-Xmx6g" \
  native:compile
```

`buildArgs` meaning:

- `--no-fallback`: fail the build instead of producing a fallback JVM image
- `-Os`: optimize for size
- `--static`: build a statically linked binary
- `--libc=musl`: link against musl (Linux/musl environments)
- `--verbose`: print detailed native-image output (useful for debugging)
- `-J-Xmx6g`: give the native-image process up to ~6GB heap

UPX compression (optional):

```bash
upx --lzma --best target/native-executable
```

- `--best`: maximum compression
- `--lzma`: use LZMA for better compression (slower, smaller)

## Observability

- Actuator endpoints: `http://localhost:8080/actuator`
- Prometheus: `http://localhost:8080/actuator/prometheus`
- OTLP exporter settings are in `application-*.yml` (disabled by default in dev)

Monitoring stack (Grafana OTEL LGTM):

```bash
docker compose -f src/main/docker/monitoring.yml up -d
```

## Docker Compose Support

Files under `src/main/docker/*.yml` are marked as “dev purpose only”.

- PostgreSQL: `docker compose -f src/main/docker/postgresql.yml up -d`
- Monitoring: `docker compose -f src/main/docker/monitoring.yml up -d`
- App with prebuilt native image (GHCR): `docker compose -f src/main/docker/app.yml up -d`

Spring Boot docker-compose integration (optional):

```bash
./mvnw -Pprod,docker-compose spring-boot:run
```

## Helm

- Chart: `helm/note-app`
- Monitoring: `helm/monitoring/lgtm.yaml`

Common commands:
Lint the chart:

```bash
helm lint helm/note-app
```

Render manifests locally:

```bash
helm template note-app helm/note-app
```

Create namespace (idempotent):

```bash
kubectl create namespace note-app --dry-run=client -o yaml | kubectl apply -f -
```

Install/upgrade release:

```bash
helm upgrade --install note-app helm/note-app -n note-app
```

Install/upgrade with values override:

```bash
helm upgrade --install note-app helm/note-app -n note-app -f helm/note-app/values.yaml
```

Uninstall release:

```bash
helm uninstall note-app -n note-app
```

## Continuous Integration

Pipeline: `.circleci/config.yml`

- `./mvnw -Pprod verify` for tests + quality gates
- `./mvnw -Pprod,native -DskipTests native:compile` for a musl static native build
- Compress `target/native-executable` with UPX
- Push native image to GHCR on the `main` branch (via Jib)

Environment variables:

- SonarCloud: `SONAR_TOKEN` (optional)
- Snyk: `SNYK_TOKEN` (optional)
- GHCR push: `GHCR_USERNAME`, `GHCR_TOKEN` (only on `main`)
