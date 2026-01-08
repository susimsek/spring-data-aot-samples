package io.github.susimsek.springdataaotsamples.web.error;

import io.github.susimsek.springdataaotsamples.service.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Stream;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Setter(onMethod_ = @Autowired)
    private MessageSource messageSource;

    @ExceptionHandler(ApiException.class)
    public @Nullable ResponseEntity<Object> handleApiException(
            ApiException ex, WebRequest request) {
        return this.handleExceptionInternal(
                ex, ex.getBody(), ex.getHeaders(), ex.getStatusCode(), request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body =
                this.buildProblemDetail(ex, status, "Validation failed",
                    "One or more validation errors occurred.",
                    null, null);
        List<Violation> violations =
                Stream.concat(
                                ex.getBindingResult().getFieldErrors().stream()
                                        .map(Violation::from),
                                ex.getBindingResult().getGlobalErrors().stream()
                                        .map(Violation::from))
                        .toList();
        body.setProperty("violations", violations);

        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public @Nullable ResponseEntity<Object> handleAuth(
            AuthenticationException ex, WebRequest request) {
        if (ex instanceof DisabledException disabled) {
            return handleDisabled(disabled, request);
        }
        if (ex instanceof OAuth2AuthenticationException oAuth2AuthenticationException) {
            return handleOAuth2Auth(oAuth2AuthenticationException, request);
        }
        return handleBadCredentials(ex, request);
    }

    private @Nullable ResponseEntity<Object> handleBadCredentials(
            AuthenticationException ex, WebRequest request) {
        ProblemDetail body =
                this.buildProblemDetail(
                        ex,
                        HttpStatus.UNAUTHORIZED,
                        "problemDetail.title.auth.badCredentials",
                        "Invalid credentials",
                        "problemDetail.auth.badCredentials",
                        null);
        return this.handleExceptionInternal(
                ex, body, HttpHeaders.EMPTY, HttpStatus.UNAUTHORIZED, request);
    }

    private @Nullable ResponseEntity<Object> handleOAuth2Auth(
            OAuth2AuthenticationException ex, WebRequest request) {
        ProblemDetail body =
                this.buildProblemDetail(
                        ex,
                        HttpStatus.UNAUTHORIZED,
                        "problemDetail.title.auth.invalidToken",
                        ex.getMessage(),
                        "problemDetail.auth.invalidToken",
                        null);
        return this.handleExceptionInternal(
                ex, body, HttpHeaders.EMPTY, HttpStatus.UNAUTHORIZED, request);
    }

    private @Nullable ResponseEntity<Object> handleDisabled(
            DisabledException ex, WebRequest request) {
        ProblemDetail body =
                this.buildProblemDetail(
                        ex,
                        HttpStatus.UNAUTHORIZED,
                        "problemDetail.title.auth.disabled",
                        "Account is disabled",
                        "problemDetail.auth.disabled",
                        null);
        return this.handleExceptionInternal(
                ex, body, HttpHeaders.EMPTY, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public @Nullable ResponseEntity<Object> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        ProblemDetail body =
                this.buildProblemDetail(
                        ex,
                        HttpStatus.FORBIDDEN,
                        "problemDetail.title.auth.accessDenied",
                        "Access is denied",
                        "problemDetail.auth.accessDenied",
                        null);
        return this.handleExceptionInternal(
                ex, body, HttpHeaders.EMPTY, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = NoResourceFoundException.class, produces = MediaType.TEXT_HTML_VALUE)
    public @Nullable String handleNotFoundHtml(
            NoResourceFoundException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorMessage", ex.getMessage());
        return "forward:/404";
    }

    @ExceptionHandler(Exception.class)
    public @Nullable ResponseEntity<Object> handleUnhandled(
            Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return this.handleExceptionInternal(
                ex,
                this.buildProblemDetail(
                        ex,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "problemDetail.title.internalServerError",
                        "An unexpected error occurred. Please try again later.",
                        "problemDetail.internalServerError",
                        null),
                HttpHeaders.EMPTY,
                HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    private ProblemDetail buildProblemDetail(
            Exception ex,
            HttpStatusCode status,
            @Nullable String titleMessageCode,
            String defaultDetail,
            @Nullable String detailMessageCode,
            Object @Nullable [] detailMessageArguments) {
        ErrorResponse.Builder builder = ErrorResponse.builder(ex, status, defaultDetail);
        if (detailMessageCode != null) {
            builder.detailMessageCode(detailMessageCode);
        }
        if (detailMessageArguments != null) {
            builder.detailMessageArguments(detailMessageArguments);
        }
        if (titleMessageCode != null) {
            builder.titleMessageCode(titleMessageCode);
        }
        return builder.build().updateAndGetBody(messageSource, LocaleContextHolder.getLocale());
    }
}
