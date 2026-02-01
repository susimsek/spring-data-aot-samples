package io.github.susimsek.springdataaotsamples.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProblemDetailTest {

    @Test
    void shouldCreateProblemDetailWithAllFields() {
        Violation violation = new Violation("Size", "User", "username", "a", "too short");
        List<Violation> violations = List.of(violation);

        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "One or more fields are invalid.",
                "/api/v1/users",
                "username",
                violations);

        assertThat(problemDetail.type()).isEqualTo("https://example.com/problem/validation_failed");
        assertThat(problemDetail.title()).isEqualTo("Validation Failed");
        assertThat(problemDetail.status()).isEqualTo(400);
        assertThat(problemDetail.detail()).isEqualTo("One or more fields are invalid.");
        assertThat(problemDetail.instance()).isEqualTo("/api/v1/users");
        assertThat(problemDetail.field()).isEqualTo("username");
        assertThat(problemDetail.violations()).isEqualTo(violations);
    }

    @Test
    void shouldCreateProblemDetailWithNullValues() {
        ProblemDetail problemDetail = new ProblemDetail(
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(problemDetail.type()).isNull();
        assertThat(problemDetail.title()).isNull();
        assertThat(problemDetail.status()).isNull();
        assertThat(problemDetail.detail()).isNull();
        assertThat(problemDetail.instance()).isNull();
        assertThat(problemDetail.field()).isNull();
        assertThat(problemDetail.violations()).isNull();
    }

    @Test
    void shouldCreateProblemDetailWithEmptyViolations() {
        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/not_found",
                "Not Found",
                404,
                "Resource not found.",
                "/api/v1/users/123",
                null,
                List.of());

        assertThat(problemDetail.violations()).isEmpty();
    }

    @Test
    void shouldImplementEqualsCorrectly() {
        Violation violation = new Violation("Size", "User", "username", "a", "too short");
        List<Violation> violations = List.of(violation);

        ProblemDetail problemDetail1 = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "One or more fields are invalid.",
                "/api/v1/users",
                "username",
                violations);

        ProblemDetail problemDetail2 = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "One or more fields are invalid.",
                "/api/v1/users",
                "username",
                violations);

        ProblemDetail problemDetail3 = new ProblemDetail(
                "https://example.com/problem/not_found",
                "Not Found",
                404,
                "Resource not found.",
                "/api/v1/users/123",
                null,
                null);

        assertThat(problemDetail1)
                .isEqualTo(problemDetail2)
                .isNotEqualTo(problemDetail3)
                .hasSameHashCodeAs(problemDetail2);
    }

    @Test
    void shouldImplementHashCodeCorrectly() {
        Violation violation = new Violation("Size", "User", "username", "a", "too short");
        List<Violation> violations = List.of(violation);

        ProblemDetail problemDetail1 = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "One or more fields are invalid.",
                "/api/v1/users",
                "username",
                violations);

        ProblemDetail problemDetail2 = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "One or more fields are invalid.",
                "/api/v1/users",
                "username",
                violations);

        assertThat(problemDetail1.hashCode()).isEqualTo(problemDetail2.hashCode());
    }

    @Test
    void shouldImplementToStringCorrectly() {
        Violation violation = new Violation("Size", "User", "username", "a", "too short");
        List<Violation> violations = List.of(violation);

        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "One or more fields are invalid.",
                "/api/v1/users",
                "username",
                violations);

        String toString = problemDetail.toString();

        assertThat(toString)
                .contains("ProblemDetail")
                .contains("https://example.com/problem/validation_failed")
                .contains("Validation Failed")
                .contains("400")
                .contains("One or more fields are invalid.")
                .contains("/api/v1/users")
                .contains("username")
                .contains("Violation");
    }

    @Test
    void shouldHandleDifferentStatusCodes() {
        ProblemDetail problemDetail400 = new ProblemDetail(
                "type", "title", 400, "detail", "instance", "field", null);
        ProblemDetail problemDetail404 = new ProblemDetail(
                "type", "title", 404, "detail", "instance", "field", null);
        ProblemDetail problemDetail500 = new ProblemDetail(
                "type", "title", 500, "detail", "instance", "field", null);

        assertThat(problemDetail400.status()).isEqualTo(400);
        assertThat(problemDetail404.status()).isEqualTo(404);
        assertThat(problemDetail500.status()).isEqualTo(500);
    }

    @Test
    void shouldHandleMultipleViolations() {
        Violation violation1 = new Violation("Size", "User", "username", "a", "too short");
        Violation violation2 = new Violation("NotBlank", "User", "email", "", "must not be blank");
        Violation violation3 = new Violation("Email", "User", "email", "invalid", "invalid email");
        List<Violation> violations = List.of(violation1, violation2, violation3);

        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/validation_failed",
                "Validation Failed",
                400,
                "Multiple fields are invalid.",
                "/api/v1/users",
                null,
                violations);

        assertThat(problemDetail.violations())
                .hasSize(3)
                .containsExactly(violation1, violation2, violation3);
    }

    @Test
    void shouldCreateProblemDetailWithMinimalFields() {
        ProblemDetail problemDetail = new ProblemDetail(
                "about:blank",
                "Internal Server Error",
                500,
                "An unexpected error occurred.",
                null,
                null,
                null);

        assertThat(problemDetail.type()).isEqualTo("about:blank");
        assertThat(problemDetail.title()).isEqualTo("Internal Server Error");
        assertThat(problemDetail.status()).isEqualTo(500);
        assertThat(problemDetail.detail()).isEqualTo("An unexpected error occurred.");
        assertThat(problemDetail.instance()).isNull();
        assertThat(problemDetail.field()).isNull();
        assertThat(problemDetail.violations()).isNull();
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        ProblemDetail problemDetail1 = new ProblemDetail(
                "type1", "title", 400, "detail", "instance", "field", null);
        ProblemDetail problemDetail2 = new ProblemDetail(
                "type2", "title", 400, "detail", "instance", "field", null);

        assertThat(problemDetail1).isNotEqualTo(problemDetail2);
    }

    @Test
    void shouldNotBeEqualWhenViolationsDiffer() {
        Violation violation1 = new Violation("Size", "User", "username", "a", "too short");
        Violation violation2 = new Violation("NotBlank", "User", "email", "", "must not be blank");

        ProblemDetail problemDetail1 = new ProblemDetail(
                "type", "title", 400, "detail", "instance", "field", List.of(violation1));
        ProblemDetail problemDetail2 = new ProblemDetail(
                "type", "title", 400, "detail", "instance", "field", List.of(violation2));

        assertThat(problemDetail1).isNotEqualTo(problemDetail2);
    }

    @Test
    void shouldHandleEmptyStrings() {
        ProblemDetail problemDetail = new ProblemDetail(
                "",
                "",
                0,
                "",
                "",
                "",
                List.of());

        assertThat(problemDetail.type()).isEmpty();
        assertThat(problemDetail.title()).isEmpty();
        assertThat(problemDetail.status()).isZero();
        assertThat(problemDetail.detail()).isEmpty();
        assertThat(problemDetail.instance()).isEmpty();
        assertThat(problemDetail.field()).isEmpty();
        assertThat(problemDetail.violations()).isEmpty();
    }

    @Test
    void shouldCreateProblemDetailForUnauthorized() {
        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/unauthorized",
                "Unauthorized",
                401,
                "Authentication is required to access this resource.",
                "/api/v1/notes",
                null,
                null);

        assertThat(problemDetail.type()).isEqualTo("https://example.com/problem/unauthorized");
        assertThat(problemDetail.title()).isEqualTo("Unauthorized");
        assertThat(problemDetail.status()).isEqualTo(401);
    }

    @Test
    void shouldCreateProblemDetailForForbidden() {
        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/forbidden",
                "Forbidden",
                403,
                "You do not have permission to access this resource.",
                "/api/v1/admin/users",
                null,
                null);

        assertThat(problemDetail.type()).isEqualTo("https://example.com/problem/forbidden");
        assertThat(problemDetail.title()).isEqualTo("Forbidden");
        assertThat(problemDetail.status()).isEqualTo(403);
    }

    @Test
    void shouldCreateProblemDetailForConflict() {
        ProblemDetail problemDetail = new ProblemDetail(
                "https://example.com/problem/conflict",
                "Conflict",
                409,
                "Username already exists.",
                "/api/v1/users",
                "username",
                null);

        assertThat(problemDetail.type()).isEqualTo("https://example.com/problem/conflict");
        assertThat(problemDetail.title()).isEqualTo("Conflict");
        assertThat(problemDetail.status()).isEqualTo(409);
        assertThat(problemDetail.field()).isEqualTo("username");
    }
}
