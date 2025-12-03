package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import io.github.susimsek.springdataaotsamples.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "auth", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Authenticates user and returns JWT access token.")
    @ApiResponse(responseCode = "200", description = "Token issued",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenDTO login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Current user", description = "Returns details for the authenticated principal.")
    @ApiResponse(responseCode = "200", description = "Current principal info",
            content = @Content(schema = @Schema(implementation = UserDTO.class)))
    @GetMapping("/me")
    public UserDTO me() {
        return authService.getCurrentUser();
    }
}
