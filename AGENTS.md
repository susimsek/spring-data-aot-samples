# AI Agent Guidelines

This repo is a “Note” sample application built with Spring Boot 4 + Spring Data JPA (Hibernate) + Envers + Liquibase. It runs on the JVM and also targets GraalVM Native Image.

## Agent MCP Usage Guidelines

- Always use the Context7 MCP server when I need library/API documentation (e.g., Java, Spring Boot, Spring Data, Hibernate, Liquibase, GraalVM), code generation, setup or configuration steps without me having to explicitly ask.

## Quick Reference

| Action                              | Command                                           |
|-------------------------------------|---------------------------------------------------|
| Run (dev, H2)                       | `./mvnw spring-boot:run`                          |
| Run (prod, PostgreSQL)              | `./mvnw -Pprod spring-boot:run`                   |
| Run (prod + docker-compose)         | `./mvnw -Pprod,docker-compose spring-boot:run`    |
| Unit tests                          | `./mvnw test`                                     |
| Full verify (tests + quality gates) | `./mvnw verify`                                   |
| Check formatting (Spotless)         | `./mvnw -DskipTests spotless:check`               |
| Apply formatting (Spotless)         | `./mvnw -DskipTests spotless:apply`               |
| Checkstyle                          | `./mvnw -DskipTests checkstyle:check`             |
| Package                             | `./mvnw -DskipTests package`                      |
| Native executable                   | `./mvnw -Pprod,native -DskipTests native:compile` |

## Requirements

- Java: `25+` (enforced via Maven Enforcer)
- Maven: use the wrapper (`./mvnw`)

## Project Structure

- Application: `src/main/java/io/github/susimsek/springdataaotsamples`
  - `config`: Spring configurations (including AOT)
    - `apidoc`: OpenAPI / Swagger UI configuration (Springdoc)
    - `cache`: caching setup (Spring Cache + Hibernate second-level cache)
    - `security`: Spring Security configuration (filter chain, method security, etc.)
  - `domain`: JPA entities and enums
  - `repository`: Spring Data repositories (including custom impls)
  - `security`: security components (tokens, principals, utilities)
  - `scheduler`: scheduled jobs (cleanup, maintenance)
  - `service`: business logic
    - `command`: write/side-effect operations
    - `query`: read/query operations
    - `dto`: request/response models (prefer `record`)
    - `exception`: service-layer exception types
    - `mapper`: MapStruct mappers
    - `spec`: JPA `Specification` builders
    - `validation`: custom constraints + validators
  - `web`: presentation layer (REST endpoints, MVC views, exception handling)
    - `error`: API error model + `@ControllerAdvice` exception mapping
- Configuration: `src/main/resources/config` (`application*.yml`)
- Liquibase: `src/main/resources/config/liquibase` (`master.xml`, `changelog/`, `data/`)
- i18n messages: `src/main/resources/i18n`
- Static web assets (UI): `src/main/resources/static`
  - JavaScript modules: `src/main/resources/static/js`
- Docker compose: `src/main/docker/*.yml`
- Helm chart: `helm/note-app`
- Tests: `src/test/java` and `src/test/resources`

## Code Style and Quality Gates

- Formatting: Spotless + `google-java-format` (AOSP).
  - Check: `./mvnw -DskipTests spotless:check`
  - Apply: `./mvnw -DskipTests spotless:apply`
- Lint: Checkstyle runs in the `validate` phase (config: `checkstyle.xml`, suppressions: `checkstyle-suppressions.xml`).
- Follow `.editorconfig` (LF, no trailing whitespace; Java indent = 4).
- TODO rule: write `TODO:` in all caps with a colon; do not include usernames in TODOs.
- When you change code: apply formatting and ensure tests pass (`./mvnw -DskipTests spotless:apply` and `./mvnw test`).
- When you add or change behavior: add/adjust unit tests for the new logic under `src/test/java`.

## Testing Guidelines

- Tests live under `src/test/java`.
- Unit tests: `*Test.java`
- Integration tests: `*IT*.java` and the `@IntegrationTest` meta-annotation.
- Run a single unit test: `./mvnw -Dtest=TokenServiceTest test`
- Run a single integration test: `./mvnw -Dit.test=NoteControllerIT failsafe:integration-test failsafe:verify`
- Performance (Gatling): `src/test/java/gatling/simulations` and `./mvnw gatling:test`

## Native Image & AOT Guidance

- Native builds rely on runtime hints in `src/main/java/.../config/aot/NativeConfig.java`.
  - If you add Liquibase resources, validators, or reflection-requiring types, update the hints accordingly.
- Place new custom validation constraints/validators under `service/validation` (the AOT scanner targets this package).

## Authentication

- Seed users for local dev: `admin/admin`, `user/user` (see `src/main/resources/static/login.html` and Liquibase seed data).
- Auth can be provided via `Authorization: Bearer <jwt>` or via cookie `AUTH-TOKEN` (see `CookieAwareBearerTokenResolver`).
- Cookies: `AUTH-TOKEN` + `REFRESH-TOKEN` are `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/` (see `CookieUtils`).
- Refresh: `/api/auth/refresh` revokes the old refresh token and issues a new access+refresh token (rotation); `/api/auth/logout` clears cookies and accepts refresh token from body or cookie.
- `rememberMe` affects refresh TTL (`refresh-token-ttl-for-remember-me` vs `refresh-token-ttl`).
- `prod` requires `SECURITY_JWT_SECRET` (at least 256-bit, e.g. `openssl rand -base64 32`).

## Development Guidelines

### Architecture

- When adding endpoints, keep the flow: `web` (controller) → `service` (command/query) → `repository`.
- Keep read/write separation: reads in `service/query`, writes in `service/command`.

### DTO Design

- DTOs live in `service/dto`; prefer `record` + Bean Validation annotations.
- Use `*Request` for input DTOs; avoid the `*Response` suffix for output types (prefer `*DTO` or a domain-specific name).
- Service layer should not return JPA entities directly; services should return DTOs (or primitives) and keep entities internal to the persistence/service implementation.

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
- If you add new message codes for error titles/details, update `src/main/resources/i18n/messages.properties` (i18n-backed `ProblemDetail`).

### Security

- APIs are authenticated by default; for public endpoints, explicitly allow them in `SecurityConfig`.
- Keep authorization checks in the service layer (e.g. `NoteAuthorizationService`, `SecurityUtils.getCurrentUserLogin()`), not in repositories.
- Admin-only endpoints should live under `web/admin` (`/api/admin/**`) and require `AuthoritiesConstants.ADMIN`.

### Data Access & Queries

- Keep query logic in `service/query` and use `service/spec` (JPA `Specification`) for composable filters.
- For pageable queries that need to fetch tags/collections, follow the `NoteRepository#findAllWithTags` pattern to avoid pagination + join pitfalls.
- Use soft delete via `SoftDeleteRepository` for “trash” semantics; reserve permanent deletes for explicit purge paths.
- If an input needs normalization (trim/lowercase/canonicalization), do it once at the right boundary:
  - Persisted fields: normalize at the entity layer via `@PrePersist` / `@PreUpdate` (see `User#normalize()`) to keep DB state consistent.
  - Non-persisted/request-only values: normalize in the DTO mapping/validation layer (and avoid duplicating the same normalization across services/controllers).

### Caching

- The project uses Spring Cache (Caffeine) and Hibernate second-level cache; use `@Cacheable` consistently and clear/evict caches on writes (see `CacheProvider`).
- Cache names/regions are centralized in `CacheConfig#cacheManagerCustomizer`.
- When introducing a new Spring cache (`@Cacheable`, `@CacheEvict`, etc.) or a Hibernate second-level cache region (`@org.hibernate.annotations.Cache`), also register the region in `CacheConfig` (otherwise `CacheProvider#clearCache` will fail and caching can become inconsistent).

### Async & Scheduling

- Async + scheduling are enabled in `AsyncConfig`; scheduled jobs live under the `scheduler` package.
- After background jobs mutate persistent state, evict relevant caches via `CacheProvider`.

### Database Migrations

- For DB changes: add a new Liquibase changelog and include it from `src/main/resources/config/liquibase/master.xml`.

### Docker Compose (Optional)

- Spring Boot Docker Compose integration is enabled only with the Maven profile `-Pdocker-compose` (dependency: `spring-boot-docker-compose`).
- Compose config: `src/main/docker/services.yml` (starts PostgreSQL for `prod`).
- `spring.docker.compose.lifecycle-management=start-only` means containers are not stopped automatically.

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
  - `400` for `@Valid` handlers is added via `OpenApiConfig`; don’t repeat it per endpoint unless you need a special case.
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
  - Use shared helpers from `src/main/resources/static/js/validation.js` (`getSizeState`, `toggleInlineMessages`, `toggleSizeMessages`, `togglePatternMessage`) instead of re-implementing size/required logic per form.
  - Keep feedback blocks consistent: `data-error-type="<inputId>-required"`, `data-error-type="<inputId>-size"`, and for regex/custom checks `data-error-type="<inputId>-pattern"`.
  - For regex/custom checks, set `input.setCustomValidity(...)` and let `toggleInlineMessages(...)` drive `is-valid`/`is-invalid`; show the pattern message via `togglePatternMessage(...)` (typically only after size passes so users see one message at a time).
  - For cross-field validation (e.g., date ranges), set `is-invalid` on the relevant inputs and show a dedicated error container (see the shared links custom date modal).
- Prefer `textContent` for rendering; when you must build HTML, use `Helpers.escapeHtml` / `Render.escapeHtml` and avoid injecting untrusted values into `innerHTML` or inline styles.
- Keep inline `style="..."` usage minimal; prefer Bootstrap utility classes.

## Commit & Pull Request Guidelines

- Keep changes focused; avoid drive-by refactors in the same PR.
- Prefer small, logically grouped commits; avoid `WIP`/“fix typo” noise.
- Commit messages: use imperative present tense and a consistent prefix (e.g., Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`).
- Before opening a PR: run formatting + at least unit tests (`./mvnw -DskipTests spotless:apply` and `./mvnw test`); use `./mvnw verify` when the change is non-trivial or touches build/AOT/security/data access; do not open a PR unless formatting and tests pass.
- PR description should include: what/why, how to verify, and any risks or follow-ups.
- Call out cross-cutting impacts explicitly: Liquibase migrations, new/changed config properties, cache regions/names, security rules (`SecurityConfig`), and AOT/native hints (`config/aot/NativeConfig.java`).
- For UI changes, include screenshots or short notes about the affected pages and any new validation rules/messages.
- Never include secrets (e.g., JWT secret) in commits/PRs; prefer env vars and document required setup in `README.md` if needed.

## Common Mistakes to Avoid

- Skipping the layer flow (`web` → `service` → `repository`) and pushing business logic into controllers.
- Forgetting `@Transactional(readOnly = true)` for query services and accidentally writing inside read paths.
- In `@NullMarked` packages, forgetting to add `@Nullable` for values that can be absent, leading to incorrect nullness assumptions.
- Mutating state without evicting/clearing affected caches (use `CacheProvider` on writes, async jobs, and schedulers).
- Adding caching annotations (`@Cacheable` or Hibernate `@Cache`) but forgetting to register the cache name/region in `CacheConfig`.
- Adding public endpoints but not updating `SecurityConfig` (or forgetting admin protection for `/api/admin/**`).
- Returning undocumented error shapes; keep API errors consistent with `ProblemDetail` + `violations`.
- Adding new validators outside `service/validation` (native/AOT scanning will miss them).
- In Mockito `verify/when/given`, avoid useless `eq(...)` matchers; pass values directly and use `ArgumentCaptor` when you need to assert arguments.
- Avoid redundant temporary variables like `var result = expr; return result;` — return the expression directly.
- Avoid unused initial assignments just to overwrite them later (e.g., `List<Long> ids = List.of();`).
- Don’t forget a `default` branch in `switch` statements.
- Using fully qualified names everywhere instead of imports (only use FQNs to resolve ambiguity).
- Rendering untrusted values via `innerHTML` without escaping (XSS risk); prefer `textContent` or escape helpers.
- Adding/altering a form input but forgetting to add HTML constraints and matching `invalid-feedback` elements (users end up with silent failures or inconsistent validation UX).
