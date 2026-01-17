# AI Agent Guidelines

This repo is a “Note” sample application built with Spring Boot 4 + Spring Data JPA (Hibernate) + Envers + Liquibase. It runs on the JVM and also targets GraalVM Native Image.

## Quick Reference

| Action                               | Command                                                  |
|--------------------------------------|----------------------------------------------------------|
| Run (dev, H2)                        | `./mvnw spring-boot:run`                                 |
| Run (prod, PostgreSQL)               | `./mvnw -Pprod spring-boot:run`                          |
| Unit tests                           | `./mvnw test`                                            |
| Full verify (includes ITs + quality) | `./mvnw verify`                                          |
| Package                              | `./mvnw -DskipTests package`                             |
| Apply formatting (Spotless)          | `./mvnw -DskipTests spotless:apply`                      |
| Native executable                    | `./mvnw -Pprod,native -DskipTests native:compile`        |
| Build container image (JVM)          | `./mvnw -Pprod -DskipTests jib:dockerBuild`              |
| Build container image (native)       | `./mvnw -Pprod,native -DskipTests jib:dockerBuild`       |

## Requirements

- Java: `25+` (enforced via Maven Enforcer)
- Maven: use the wrapper (`./mvnw`)

## Project Structure

- Application: `src/main/java/io/github/susimsek/springdataaotsamples`
  - `config`: Spring configurations (including AOT)
    - `apidoc`: OpenAPI / Swagger UI configuration (Springdoc)
    - `cache`: caching setup (Spring Cache + Hibernate second-level cache via JCache)
    - `security`: Spring Security configuration (filter chain, method security, etc.)
  - `domain`: JPA entities and enums
  - `repository`: Spring Data repositories (including custom impls)
  - `security`: Security components (services, tokens, principals, utilities)
  - `scheduler`: Scheduled jobs (cleanup, maintenance)
  - `service`: business logic
    - `command`: write/side-effect operations
    - `query`: read/query operations
    - `dto`: request/response models (mostly `record`)
    - `exception`: service-layer exception types
    - `mapper`: MapStruct mappers
    - `spec`: JPA `Specification` builders
    - `validation`: custom constraints + validators
  - `web`: Presentation layer (REST endpoints, MVC views, and exception handling)
    - `error`: API error model + `@ControllerAdvice` exception mapping
- Configuration: `src/main/resources/config` (`application*.yml`)
- Static web assets (UI): `src/main/resources/static` (served HTML pages, icons, and other static resources)
  - JavaScript modules: `src/main/resources/static/js`
- i18n messages: `src/main/resources/i18n` (e.g. `messages.properties`)
- Liquibase: `src/main/resources/config/liquibase` (`master.xml`, `changelog/`, `data/`)
- Docker compose: `src/main/docker/*.yml`
- Helm chart: `helm/note-app`
- Tests: `src/test/java` and `src/test/resources`

## Code Style and Quality Gates

- Formatting: Spotless + `google-java-format` (AOSP).
  - Check: `./mvnw -DskipTests spotless:check`
  - Apply: `./mvnw -DskipTests spotless:apply`
- Lint: Checkstyle runs in the `validate` phase (config: `checkstyle.xml`, suppressions: `checkstyle-suppressions.xml`).
- Follow `.editorconfig` (LF, no trailing whitespace, Java indent = 4).
- TODO rule: write `TODO:` in all caps with a colon; do not include usernames in TODOs.

## Testing Guide

- Tests live under `src/test/java`.
- Unit tests: `*Test.java`
- Integration tests: `*IT*.java` and the `@IntegrationTest` meta-annotation
- Performance (Gatling): `src/test/java/gatling/simulations` and `./mvnw gatling:test`

## Native Image & AOT Guidance

- Native builds rely on runtime hints in `src/main/java/.../config/aot/NativeConfig.java`.
  - If you add Liquibase resources, validators, or reflection-requiring types, update the hints accordingly.
- Place new custom validation constraints/validators under `service/validation` (the scanner targets this package).

## Development Guideline

### Architecture

- When adding endpoints, keep the flow: `web` (controller) → `service` (command/query) → `repository`.
- Keep read/write separation: reads in `service/query`, writes in `service/command`.

### DTO Design

- DTOs live in `service/dto`; prefer `record` + Bean Validation annotations.

### Null Safety

- The codebase uses JSpecify `@NullMarked` in `package-info.java` files, so types are non-null by default.
- When a value can be absent, use `org.jspecify.annotations.Nullable` (and avoid returning/passing raw `null` unless annotated).

### Import Conventions

- Prefer `import` statements and short class names in code.
- Use fully qualified names only to resolve ambiguity (e.g., two different classes with the same simple name); in that case, import one and use a fully qualified name for the other only where needed.

### Transaction Management

- Put transaction boundaries in the service layer (`@Transactional` on `service/*`); use `@Transactional(readOnly = true)` for query paths.
- Avoid `@Transactional` in controllers; controllers should orchestrate request/response only.
- For background work (`@Async` / schedulers), remember Spring proxy rules (no self-invocation); use `Propagation.REQUIRES_NEW` when you need an isolated transaction.

### Error Handling

- Prefer throwing `service/exception/*` (extends `ApiException`) for domain/service errors; `web/error/GlobalExceptionHandler` maps them to `ProblemDetail`.
- Use Bean Validation (`@Valid`) for request DTOs; validation failures are returned as `ProblemDetail` with `violations`.
- If you add new message codes for error titles/details, update `src/main/resources/i18n`.

### Security

- APIs are authenticated by default; for public endpoints, explicitly allow them in `SecurityConfig`.
- Keep authorization checks in the service layer (e.g. `NoteAuthorizationService`, `SecurityUtils.getCurrentUserLogin()`), not in repositories.
- Admin-only endpoints should live under `web/admin` (`/api/admin/**`) and require `AuthoritiesConstants.ADMIN`.

### Data Access & Queries

- Keep query logic in `service/query` and use `service/spec` (JPA `Specification`) for composable filters.
- For pageable queries that need to fetch tags/collections, follow the `NoteRepository#findAllWithTags` pattern to avoid pagination + join pitfalls.
- Use soft delete via `SoftDeleteRepository` for “trash” semantics; reserve permanent deletes for explicit purge paths.

### Caching

- The project uses Spring Cache (Caffeine) and Hibernate second-level cache; use `@Cacheable` consistently and clear/evict caches on writes (see `CacheProvider`).
- Cache names/regions are centralized in `CacheConfig#cacheManagerCustomizer`.
- When introducing a new Spring cache (`@Cacheable`, `@CacheEvict`, etc.) or a Hibernate second-level cache region (`@org.hibernate.annotations.Cache`), also register the region in `CacheConfig` (otherwise `CacheProvider#clearCache` will fail and caching can become inconsistent).

### Async & Scheduling

- Async + scheduling are enabled in `AsyncConfig`; scheduled jobs live under the `scheduler` package.
- After background jobs mutate persistent state, evict relevant caches via `CacheProvider`.

### Database Migrations

- For DB changes: add a new Liquibase changelog and include it from `src/main/resources/config/liquibase/master.xml`.

### Configuration Properties

- `application.*` config is strongly typed in `ApplicationProperties` (`ignoreUnknownFields=false`), with defaults in `ApplicationDefaults`.
- When adding new properties, update `ApplicationProperties`, `ApplicationDefaults`, and the relevant `application*.yml` files together.

### API Documentation (OpenAPI / Swagger UI)

- Springdoc is enabled in `dev` and disabled in `prod` (see `application-dev.yml` and `application-prod.yml`).
- Local endpoints:
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI base config lives in `OpenApiConfig` and is driven by `application.api-docs.*` properties (see `ApplicationProperties`).
- Security:
  - The OpenAPI spec defines a global `bearer-jwt` security scheme; endpoints are considered secured by default.
  - For public endpoints (no auth), annotate the handler with `@SecurityRequirements` (no values) to clear the global requirement (see `AuthenticationController`).
  - `OpenApiConfig#securityResponsesCustomizer` automatically adds `401/403` and a default `500` response to secured operations; for `@SecurityRequirements` endpoints, document those responses explicitly when applicable.
- Annotation conventions:
  - Put `@Tag(name=..., description=...)` at controller level and `@Operation(summary=..., description=...)` at handler level.
  - Document parameters with `@Parameter` (path/query) and use `@ParameterObject` for `Pageable` so paging/sorting appear in Swagger UI.
  - Add `@ApiResponse` entries for non-2xx outcomes you can return (e.g. `400/404/409`) and use `web.error.ProblemDetail` as the documented error schema for consistency with the global examples.
- DTO schema:
  - DTOs live in `service/dto`; add `@Schema(description=..., example=...)` on record components that are exposed via the API.
  - For enums exposed via the API, add `@Schema` on the enum (and/or constants) to make values and meaning clear.

### Static UI (HTML/JS)

- Static pages live under `src/main/resources/static` and use Bootstrap + Font Awesome via WebJars; there is no custom `.css` file today.
- JavaScript is plain ES modules under `src/main/resources/static/js` (no bundler); import via relative paths and load entrypoints with `type="module"`.
- Theme handling is centralized in `Theme` (`localStorage` key `theme`); include `/js/theme.js` early to avoid a flash of wrong theme and call `Theme.init(...)` on pages with a toggle.
- API calls from the UI should go through `Api` (`/js/api.js`); it handles JSON parsing, consistent `ApiError`, and a 401 → refresh → retry flow (cookie-based auth).
- Forms and client-side validation:
  - Prefer HTML5 constraints (`required`, `minlength`/`maxlength`, `type`, etc.) and keep forms `novalidate` when using custom display logic.
  - Use `Validation.toggleInlineMessages` / `Validation.toggleSizeMessages` and `invalid-feedback` blocks for consistent UX; the convention is `data-error-type="<inputId>-required"` and `data-error-type="<inputId>-size"`.
  - For cross-field validation (e.g., date ranges), set `is-invalid` on the relevant inputs and show a dedicated error container (see the shared links custom date modal).
- Prefer `textContent` for rendering; when you must build HTML, use `Helpers.escapeHtml` / `Render.escapeHtml` and avoid injecting untrusted values into `innerHTML` or inline styles.
- Keep inline `style="..."` usage minimal; prefer Bootstrap utility classes.

## Common Mistakes to Avoid

- Skipping the layer flow (`web` → `service` → `repository`) and pushing business logic into controllers.
- Forgetting `@Transactional(readOnly = true)` for query services and accidentally writing inside read paths.
- In `@NullMarked` packages, forgetting to add `@Nullable` for values that can be absent, leading to incorrect nullness assumptions.
- Mutating state without evicting/clearing affected caches (use `CacheProvider` on writes, async jobs, and schedulers).
- Adding caching annotations (`@Cacheable` or Hibernate `@Cache`) but forgetting to register the cache name/region in `CacheConfig`.
- Adding public endpoints but not updating `SecurityConfig` (or forgetting admin protection for `/api/admin/**`).
- Returning undocumented error shapes; keep API errors consistent with `ProblemDetail` + `violations`.
- Adding new validators outside `service/validation` (native/AOT scanning will miss them).
- Using fully qualified names everywhere instead of imports (only use FQNs to resolve ambiguity).
- Rendering untrusted values via `innerHTML` without escaping (XSS risk); prefer `textContent` or escape helpers.
- Adding/altering a form input but forgetting to add HTML constraints and matching `invalid-feedback` elements (users end up with silent failures or inconsistent validation UX).
