package io.github.susimsek.springdataaotsamples.service.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException(String email) {
        super(
                HttpStatus.CONFLICT,
                "Email already exists",
                "Email already exists: " + email,
                email);
    }
}
