package io.github.susimsek.springdataaotsamples.web.error;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public record Violation(
        String code,
        String field,
        Object rejectedValue,
        String message) {

    public static Violation from(FieldError error) {
        return new Violation(error.getCode(),
            error.getField(),
            error.getRejectedValue(),
            error.getDefaultMessage());
    }

    public static Violation from(ObjectError error) {
        return new Violation(error.getCode(),
            error.getObjectName(),
            null,
            error.getDefaultMessage());
    }
}
