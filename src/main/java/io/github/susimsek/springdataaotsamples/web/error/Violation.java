package io.github.susimsek.springdataaotsamples.web.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

@Schema(name = "Violation", description = "Details of a single validation failure")
public record Violation(
        @JsonProperty @Schema(description = "Validation rule code that failed", example = "size")
                String code,
        @JsonProperty("object")
                @Schema(
                        description = "Name of the validated object",
                        example = "generateArticleRequest")
                String objectName,
        @JsonProperty @Schema(description = "Field name that failed validation", example = "title")
                String field,
        @JsonProperty @Schema(description = "Value that was rejected", example = "x")
                Object rejectedValue,
        @JsonProperty
                @Schema(
                        description = "Reason why validation failed",
                        example = "Field must be between 3 and 50 characters.")
                String message) {

    public static Violation from(FieldError error) {
        return new Violation(
                error.getCode(),
                error.getObjectName().replaceFirst("DTO$", ""),
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage());
    }

    public static Violation from(ObjectError error) {
        return new Violation(
                error.getCode(),
                error.getObjectName().replaceFirst("DTO$", ""),
                error.getObjectName(),
                null,
                error.getDefaultMessage());
    }
}
