package io.github.susimsek.springdataaotsamples.service.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

public abstract class ApiException extends RuntimeException implements ErrorResponse {

    private final HttpStatusCode status;
    private final HttpHeaders headers;
    private final ProblemDetail body;
    private final transient Object[] args;

    protected ApiException(HttpStatusCode status, String title, String detail, Object... args) {
        super(detail);
        this.status = status;
        this.headers = HttpHeaders.EMPTY;
        this.body = ProblemDetail.forStatusAndDetail(status, detail);
        this.body.setTitle(title);
        this.args = args;
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return status;
    }

    @Override
    public ProblemDetail getBody() {
        return body;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public Object @Nullable [] getDetailMessageArguments() {
        return args;
    }
}
