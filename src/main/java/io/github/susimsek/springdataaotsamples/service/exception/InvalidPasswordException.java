package io.github.susimsek.springdataaotsamples.service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidPasswordException extends ApiException {

    private final String detailMessageCode;

    public InvalidPasswordException(String detailMessageCode, String defaultDetail) {
        super(HttpStatus.BAD_REQUEST, "Invalid password", defaultDetail);
        this.detailMessageCode = detailMessageCode;
    }

    @Override
    public String getTitleMessageCode() {
        return "problemDetail.title.invalidPassword";
    }

    @Override
    public String getDetailMessageCode() {
        return detailMessageCode;
    }
}
