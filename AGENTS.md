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
  - `domain`: JPA entities and enums
  - `repository`: Spring Data repositories (including custom impls)
  - `security`: JWT/auth helpers and services
  - `service`: business logic
    - `command`: write/side-effect operations
    - `query`: read/query operations
    - `dto`: request/response models (mostly `record`)
    - `mapper`: MapStruct mappers
    - `spec`: JPA `Specification` builders
    - `validation`: custom constraints + validators
  - `web`: REST controllers and view controllers
- Configuration: `src/main/resources/config` (`application*.yml`)
- Static web assets (UI): `src/main/resources/static` (served HTML pages, icons, and other static resources)
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

- When adding endpoints, keep the flow: `web` (controller) → `service` (command/query) → `repository`.
- Keep read/write separation: reads in `service/query`, writes in `service/command`.
- DTOs live in `service/dto`; prefer `record` + Bean Validation annotations.
- For DB changes: add a new Liquibase changelog and include it from `src/main/resources/config/liquibase/master.xml`.
