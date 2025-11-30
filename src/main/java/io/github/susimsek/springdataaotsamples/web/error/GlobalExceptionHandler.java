package io.github.susimsek.springdataaotsamples.web.error;

import io.github.susimsek.springdataaotsamples.service.exception.ApiException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<@NonNull Object> handleApiException(
        @NonNull ApiException ex,
        @NonNull WebRequest request) {
        return this.handleExceptionInternal(ex, null,
            ex.getHeaders(),
            ex.getStatusCode(), request);
    }

    @Override
    protected ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        ProblemDetail body = this.createProblemDetail(
            ex, status, "Validation failed", null, (Object[])null, request);
        List<Violation> violations = Stream.concat(
            ex.getBindingResult().getFieldErrors().stream().map(Violation::from),
            ex.getBindingResult().getGlobalErrors().stream().map(Violation::from)
        ).toList();
        body.setProperty("violations", violations);

        return this.handleExceptionInternal(ex, body, headers, status, request);
    }
}
