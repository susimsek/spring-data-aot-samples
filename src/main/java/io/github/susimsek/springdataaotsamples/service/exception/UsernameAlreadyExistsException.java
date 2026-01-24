package io.github.susimsek.springdataaotsamples.service.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApiException {

    public UsernameAlreadyExistsException(String username) {
        super(
                HttpStatus.CONFLICT,
                "Username already exists",
                "Username already exists: " + username,
                username);
        setField("username");
    }
}
