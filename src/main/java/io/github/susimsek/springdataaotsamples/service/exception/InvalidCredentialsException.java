package io.github.susimsek.springdataaotsamples.service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidCredentialsException extends ApiException {

    private final String detailMessageCode;
    private final String defaultDetail;

    public InvalidCredentialsException(String detailMessageCode, String defaultDetail) {
        super(HttpStatus.BAD_REQUEST, "Invalid password", defaultDetail);
        this.detailMessageCode = detailMessageCode;
        this.defaultDetail = defaultDetail;
    }
}
