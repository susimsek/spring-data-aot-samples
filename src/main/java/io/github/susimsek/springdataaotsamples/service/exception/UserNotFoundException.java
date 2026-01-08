package io.github.susimsek.springdataaotsamples.service.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(String username) {
        super(
                HttpStatus.NOT_FOUND,
                "User not found",
                "User not found with username: " + username,
                username);
    }
}
