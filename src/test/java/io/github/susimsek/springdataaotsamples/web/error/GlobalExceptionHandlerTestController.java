package io.github.susimsek.springdataaotsamples.web.error;

import io.github.susimsek.springdataaotsamples.service.exception.InvalidCredentialsException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestController
@RequestMapping("/test")
class GlobalExceptionHandlerTestController {

    @GetMapping("/api")
    Object api() {
        throw new NoteNotFoundException(1L);
    }

    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    Object validate(@Valid @RequestBody ValidationRequest request) {
        return request;
    }

    @GetMapping("/auth/bad")
    Object badCredentials() {
        throw new BadCredentialsException("bad credentials");
    }

    @GetMapping("/auth/disabled")
    Object disabled() {
        throw new DisabledException("disabled");
    }

    @GetMapping("/auth/oauth2")
    Object oauth2() {
        throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token"), "Token is invalid");
    }

    @GetMapping("/forbidden")
    Object forbidden() {
        throw new AccessDeniedException("denied");
    }

    @GetMapping("/unhandled")
    Object unhandled() {
        throw new IllegalStateException("boom");
    }

    @GetMapping("/data-integrity")
    Object dataIntegrity() {
        throw new DataIntegrityViolationException("duplicate");
    }

    @GetMapping("/invalid-credentials/current-password")
    Object invalidCredentialsCurrentPassword() {
        throw new InvalidCredentialsException(
                "problemDetail.invalidCredentials.currentPassword",
                "Current password is incorrect.");
    }

    @GetMapping("/html-notfound")
    Object htmlNotFound() throws NoResourceFoundException {
        throw new NoResourceFoundException(HttpMethod.GET, "/missing", "Not found");
    }

    record ValidationRequest(@NotBlank String name) {}
}
