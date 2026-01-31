# AI Agent Guidelines

This repo is a “Note” sample application built with Spring Boot 4 + Spring Data JPA (Hibernate) + Envers + Liquibase. It runs on the JVM and also targets GraalVM Native Image.

## Table of Contents

1.  [Agent MCP Usage Guidelines](#agent-mcp-usage-guidelines)
2.  [Quick Reference](#quick-reference)
3.  [Prerequisites](#prerequisites)
4.  [Project Structure](#project-structure)
5.  [Code Style and Quality Gates](#code-style-and-quality-gates)
6.  [Testing Guidelines](#testing-guidelines)
7.  [Native Image & AOT Guidance](#native-image--aot-guidance)
8.  [Authentication](#authentication)
9.  [Development Guidelines](#development-guidelines)
10. [Pull Request & Commit Guidelines](#pull-request--commit-guidelines)
11. [Review Process & What Reviewers Look For](#review-process--what-reviewers-look-for)
12. [Common Mistakes to Avoid](#common-mistakes-to-avoid)

## Agent MCP Usage Guidelines

- Always use the Context7 MCP server when I need library/API documentation (e.g., Java, Spring Boot, Spring Data, Hibernate, Liquibase, GraalVM), code generation, setup or configuration steps without me having to explicitly ask.

## Quick Reference

| Action                              | Command                                           |
| ----------------------------------- | ------------------------------------------------- |
| Run (dev, H2)                       | `./mvnw spring-boot:run`                          |
| Run (prod, PostgreSQL)              | `./mvnw -Pprod spring-boot:run`                   |
| Run (prod + docker-compose)         | `./mvnw -Pprod,docker-compose spring-boot:run`    |
| Unit tests                          | `./mvnw test`                                     |
| Full verify (tests + quality gates) | `./mvnw verify`                                   |
| Check formatting (Spotless)         | `./mvnw spotless:check`                           |
| Apply formatting (Spotless)         | `./mvnw spotless:apply`                           |
| Checkstyle                          | `./mvnw checkstyle:check`                         |
| Package                             | `./mvnw -DskipTests package`                      |
| Native executable                   | `./mvnw -Pprod,native -DskipTests native:compile` |
| Backend start (dev)                 | `npm run backend:start`                           |
| Frontend dev (Next.js)              | `npm run dev`                                     |
| Frontend build (static export)      | `npm run build`                                   |
| Frontend lint (ESLint)              | `npm run lint`                                    |
| Frontend unit tests (Jest)          | `npm test`                                        |
| Frontend format apply (Prettier)    | `npm run format`                                  |

### Prerequisites

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
- Frontend (Next.js Pages Router, TypeScript): `src/main/webapp`
  - `pages`: routing (Pages Router)
    - `[locale]`: locale-prefixed pages (static export outputs `/en/login/index.html`, etc.)
    - `index.tsx`: root entry redirect (`/` → `/{locale}/...`)
    - Top-level route shims: `login.tsx`, `register.tsx`, `share.tsx`, etc. (redirect to `/{locale}/...`)
    - `_app.tsx`: app wrapper/providers
  - `components`: shared UI components (navbar, auth guard, theme, etc.)
  - `lib`: shared frontend code
    - `api.ts`: Axios client (auth refresh + headers)
    - `auth.ts`: auth/token helpers + storage
    - `getStatic.ts`: next-i18next SSG helpers (`getStaticPaths`, `makeStaticProps`)
    - `languageDetector.ts`: locale detection + cache
    - `redirect.tsx`: locale redirect helpers (used by root/shim pages)
    - `routes.ts`: route constants/builders
    - `store.ts`: Redux store + typed hooks
    - `types.ts`: shared DTO/types (frontend)
    - `useAuth.ts`: auth hooks
    - `window.ts`: browser globals wrappers (SSR-safe)
  - `slices`: Redux slices (`authSlice`, `themeSlice`)
  - `styles`: global CSS (`styles.css`)
  - `public`: static assets
    - `locales/<lng>/<ns>.json`: next-i18next translation files
  - `__tests__`: shared frontend test helpers
  - `next.config.ts`: Next config (static export: `output: 'export'`, `trailingSlash: true`, output dir: `build/`)
  - `next-env.d.ts`: Next-generated TS types (do not edit manually)
  - `build`: Next static export output (Maven copies to `target/classes/static`; see `pom.xml` `copy-frontend-build`)
  - `.next`: Next build cache/types output (generated)
- Frontend i18n config (next-i18next): `next-i18next.config.js` (repo root; `localePath` points at `src/main/webapp/public/locales`)
- Docker compose: `src/main/docker/*.yml`
- Helm chart: `helm/note-app`
- Backend tests: `src/test/java` and `src/test/resources`

## Code Style and Quality Gates

### Backend

- Formatting: Spotless + `google-java-format` (AOSP).
  - Check: `./mvnw spotless:check`
  - Apply: `./mvnw spotless:apply`
- Lint: Checkstyle runs in the `validate` phase (config: `checkstyle.xml`, suppressions: `checkstyle-suppressions.xml`).
- Follow `.editorconfig` (LF, no trailing whitespace; Java indent = 4).
- TODO rule: write `TODO:` in all caps with a colon; do not include usernames in TODOs.
- When you change code: apply formatting and ensure tests pass (`./mvnw spotless:apply` and `./mvnw test`).
- When you add or change behavior: add/adjust unit tests for the new logic under `src/test/java`.

### Frontend

- Frontend lint/format (Next.js under `src/main/webapp`):
  - Lint (ESLint): `npm run lint`
  - Unit tests (Jest): `npm test`
  - Format apply (Prettier): `npm run format`
  - ESLint config: `eslint.config.mjs`
  - Prettier config: `.prettierrc.json` (ignores in `.prettierignore`)
  - TypeScript config: `tsconfig.json`
- When you change frontend code: apply formatting and ensure lint/tests pass (`npm run format`, `npm run lint`, `npm test`).
- When you add or change frontend behavior: add/adjust unit tests for the new logic under `src/main/webapp`.

## Testing Guidelines

### Unit Tests

#### Backend

- Backend unit tests live under `src/test/java` and follow `*Test.java`.
- Run a single backend unit test:
  - `./mvnw -Dtest=TokenServiceTest test`

#### Frontend

- Frontend unit tests (Jest) live under `src/main/webapp` (e.g., `src/main/webapp/**/*.test.ts`, `src/main/webapp/**/*.test.tsx`).
- Run frontend unit tests:
  - `npm test`
- Run a single frontend test file:
  - `npm test -- src/main/webapp/app/components/AppNavbar.test.tsx`
- Run a single frontend test by name:
  - `npm test -- -t "ThemeToggleButton"`

### Integration Tests

- Integration tests live under `src/test/java` and follow `*IT*.java` and the `@IntegrationTest` meta-annotation.
- Run a single integration test:
  - `./mvnw -Dit.test=NoteControllerIT failsafe:integration-test failsafe:verify`

### Performance Tests

- Performance tests (Gatling) live under `src/test/java/gatling/simulations`.
- Run Gatling:
  - `./mvnw gatling:test`

## Native Image & AOT Guidance

- Native builds rely on runtime hints in `src/main/java/.../config/aot/NativeConfig.java`.
  - If you add Liquibase resources, validators, or reflection-requiring types, update the hints accordingly.
- Place new custom validation constraints/validators under `service/validation` (the AOT scanner targets this package).

## Authentication

- Seed users for local dev: `admin/admin`, `user/user` (see Liquibase seed data).
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

### Frontend

- Source lives under `src/main/webapp` (App Router under `src/main/webapp/app`), but dependencies are managed at repo root via npm workspaces.
- Frontend is written in TypeScript (`.ts`/`.tsx`); TypeScript config lives at repo root (`tsconfig.json`) and is referenced from `src/main/webapp/next.config.js`.
- Production build uses a static export (`output: 'export'`) with `distDir: 'build'` (see `src/main/webapp/next.config.js`).
  - Maven runs `npm ci` + `npm run build` via `frontend-maven-plugin` (Node installed under repo root `node/`) and copies `src/main/webapp/build` to `target/classes/static` via `maven-resources-plugin` (see `pom.xml`).
- Routing is owned by Next.js; backend forwards unknown non-API routes to the corresponding `.html` file via `SpaWebFilter` registered in the Spring Security filter chain (see `SecurityConfig`).
- Authentication redirects are handled client-side by Next.js via `AuthGuard` component and `routes.ts` configuration (see `src/main/webapp/app/components/AuthGuard.tsx` and `src/main/webapp/app/lib/routes.ts`).
- API calls in the Next UI should go through `src/main/webapp/app/lib/api.ts` (relative `/api/...` URLs).
  - Keep auth failure handling generic: a 401 triggers refresh+retry; if refresh fails, redirect to `/login`. Avoid per-page `if (err.status === 401) ...` blocks.
- Dev proxy: `src/main/webapp/next.config.js` defines `rewrites()` in dev to proxy `/api/**` to the Spring Boot backend.
- CSP note: Next static export emits inline scripts; a strict `script-src 'self'` CSP will break the UI. If you tighten CSP, you must handle inline scripts via nonces/hashes (or avoid static export).
- Shared UI: use `src/main/webapp/app/components/AppNavbar.tsx` instead of creating page-specific navbar components.

## Pull Request & Commit Guidelines

- Keep changes focused; avoid drive-by refactors in the same PR.
- Prefer small, logically grouped commits; avoid `WIP`/“fix typo” noise.
- Never include secrets (e.g., JWT secret) in commits/PRs; prefer env vars and document required setup in `README.md`.

- Use **Conventional Commits**:
  - `feat`: new feature
  - `fix`: bug fix
  - `docs`: documentation only
  - `test`: adding or fixing tests
  - `chore`: build, CI, or tooling changes
  - `perf`: performance improvement
  - `refactor`: code changes without feature or fix
  - `build`: changes that affect the build system
  - `ci`: CI configuration
  - `style`: code style (formatting, missing semicolons, etc.)
  - `types`: type-related changes
  - `revert`: reverts a previous commit

- Commit message format:

  ```
  <type>(<scope>): <short summary>

  Optional longer description.
  ```

- Keep summary under 80 characters. Use imperative present tense.

- Before opening a PR:
  - Always apply formatting and run at least unit tests:
    - Backend: `./mvnw spotless:apply` and `./mvnw test`
    - Frontend changes under `src/main/webapp`: `npm run format`, `npm run lint`, `npm test`
  - Do not open a PR unless formatting and the relevant tests pass.

- PR title: use a Conventional Commit-style title (same as commit summary).

- PR description should include:
  - What changed and why.
  - How to verify (exact commands and/or steps).
  - Any risks, rollback notes, or follow-ups.

- Call out cross-cutting impacts explicitly when relevant:
  - Liquibase migrations
  - New/changed config properties
  - Cache regions/names
  - Security rules (`SecurityConfig`)
  - AOT/native hints (`config/aot/NativeConfig.java`)

## Review Process & What Reviewers Look For

### General (Applies to All PRs)

- ✅ All automated checks pass (backend build/tests/format + frontend lint/tests when applicable).
- ✅ Changes are focused and minimal; no unrelated refactors or drive-by cleanups.
- ✅ Commit history is clean, logical, and follows Conventional Commits.
- ✅ No secrets, credentials, or environment-specific values are committed.
- ✅ PR description clearly explains:
  - What changed and why
  - How to verify
  - Risks or follow-ups (if any)

### Backend Review Checklist

- ✅ Maven build and tests pass (run `./mvnw verify` when the change is non-trivial).
- ✅ Unit and/or integration tests are added or updated to cover new behavior and edge cases.
- ✅ Code follows the established architecture: `web → service → repository`.
- ✅ Transaction boundaries are correct (`@Transactional`, `readOnly = true` where appropriate).
- ✅ Error handling remains consistent with `ProblemDetail` and validation standards.
- ✅ Public-facing API changes (REST endpoints, DTOs, enums, config properties) are documented or clearly explained.
- ✅ Cross-cutting impacts are explicitly called out when applicable:
  - Liquibase migrations
  - Cache regions / eviction behavior
  - Security rules (`SecurityConfig`)
  - Native Image / AOT hints (`NativeConfig`)

### Frontend Review Checklist

- ✅ Frontend build, lint, and tests pass when UI code is touched (`npm run build`, `npm run lint`, `npm test`).
- ✅ Unit tests are added or updated for new UI logic or components.
- ✅ UI/UX changes include brief notes or screenshots when behavior or visuals change.
- ✅ API usage goes through the shared API layer (`src/main/webapp/app/lib/api.ts`).
- ✅ Authentication and error handling follow the existing global patterns (avoid per-page special-casing).
- ✅ TypeScript types are strict and consistent (`Readonly<Props>`, avoid `any`).
- ✅ No unsafe rendering patterns are introduced (avoid `innerHTML` unless sanitized).

## Common Mistakes to Avoid

### Backend

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

### Frontend

- Rendering untrusted values via `innerHTML` without escaping (XSS risk); prefer `textContent` or escape helpers.
- Adding/altering a form input but forgetting to add HTML constraints and matching `invalid-feedback` elements (users end up with silent failures or inconsistent validation UX).
- In TypeScript React components, keep props types read-only (e.g., `Readonly<Props>`); use this pattern consistently.
- In TypeScript, prefer `globalThis` over `window`/`self`/`global` for globals; it keeps code environment-agnostic.
