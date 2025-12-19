package io.github.susimsek.springdataaotsamples.service.exception;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

@NullMarked
public class UserNotFoundException extends ApiException {

  public UserNotFoundException(String username) {
    super(
        HttpStatus.NOT_FOUND,
        "User not found",
        "User not found with username: " + username,
        username);
  }
}
