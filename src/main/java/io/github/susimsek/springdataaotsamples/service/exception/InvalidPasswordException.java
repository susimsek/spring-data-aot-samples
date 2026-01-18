package io.github.susimsek.springdataaotsamples.service.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends ApiException {

    private final String detailMessageCode;
    private final String defaultDetail;

    public InvalidPasswordException(String detailMessageCode, String defaultDetail) {
        super(HttpStatus.BAD_REQUEST, "Invalid password", defaultDetail);
        this.detailMessageCode = detailMessageCode;
        this.defaultDetail = defaultDetail;
    }

    public String getDetailMessageCode() {
        return detailMessageCode;
    }

    public String getDefaultDetail() {
        return defaultDetail;
    }
}
