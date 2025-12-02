package io.github.susimsek.springdataaotsamples.web.error;

import io.github.susimsek.springdataaotsamples.service.exception.ApiException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Setter(onMethod_ = @Autowired)
    private MessageSource messageSource;

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
            ex, status, "Validation failed",
            null, null, null, request);
        List<Violation> violations = Stream.concat(
            ex.getBindingResult().getFieldErrors().stream().map(Violation::from),
            ex.getBindingResult().getGlobalErrors().stream().map(Violation::from)
        ).toList();
        body.setProperty("violations", violations);

        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull Object> handleUnhandled(
        @NonNull Exception ex,
        @NonNull WebRequest request
    ) {
        return this.handleExceptionInternal(
            ex,
            this.createProblemDetail(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                "problemDetail.internalServerError",
                null,
                "problemDetail.title.internalServerError",
                request
            ),
            HttpHeaders.EMPTY,
            HttpStatus.INTERNAL_SERVER_ERROR,
            request);
    }

    private ProblemDetail createProblemDetail(
        Exception ex,
        HttpStatusCode status,
        String defaultDetail,
        String detailMessageCode,
        Object[] detailMessageArguments,
        String titleMessageCode,
        WebRequest request
    ) {
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
