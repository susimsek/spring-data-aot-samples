package io.github.susimsek.springdataaotsamples.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GlobalExceptionHandlerTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MessageSource messageSource;

    @BeforeEach
    void setupMessageSource() {
        when(messageSource.getMessage(any(MessageSourceResolvable.class), any(Locale.class)))
                .thenAnswer(
                        invocation -> {
                            MessageSourceResolvable resolvable = invocation.getArgument(0);
                            if (resolvable.getDefaultMessage() != null) {
                                return resolvable.getDefaultMessage();
                            }
                            String[] codes = resolvable.getCodes();
                            return (codes != null && codes.length > 0) ? codes[0] : "";
                        });
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(
                        invocation -> {
                            String code = invocation.getArgument(0);
                            String defaultMessage = invocation.getArgument(2);
                            return defaultMessage != null ? defaultMessage : code;
                        });
    }

    @Test
    void shouldHandleApiException() throws Exception {
        mockMvc.perform(get("/test/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Note not found"))
                .andExpect(jsonPath("$.detail").value("Note not found with id: 1"));
    }

    @Test
    void shouldAddViolationsForValidationErrors() throws Exception {
        mockMvc.perform(
                        post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations[0].field").value("name"));
    }

    @Test
    void shouldHandleBadCredentials() throws Exception {
        mockMvc.perform(get("/test/auth/bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void shouldHandleDisabledAccount() throws Exception {
        mockMvc.perform(get("/test/auth/disabled"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Account is disabled"));
    }

    @Test
    void shouldHandleOAuth2AuthenticationException() throws Exception {
        mockMvc.perform(get("/test/auth/oauth2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Token is invalid"));
    }

    @Test
    void shouldHandleAccessDenied() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access is denied"));
    }

    @Test
    void shouldHandleUnhandledExceptions() throws Exception {
        mockMvc.perform(get("/test/unhandled"))
                .andExpect(status().isInternalServerError())
                .andExpect(
                        jsonPath("$.detail")
                                .value("An unexpected error occurred. Please try again later."));
    }

    @Test
    void shouldHandleDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.detail")
                                .value("The request violates a data integrity constraint."));
    }

    @Test
    void shouldHandleInvalidCredentials() throws Exception {
        mockMvc.perform(get("/test/invalid-credentials/current-password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect."));
    }

    @Test
    void shouldForwardTo404PageForHtmlRequests() throws Exception {
        mockMvc.perform(get("/test/html-notfound").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl("/404"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void createProblemDetailShouldPassDetailMessageArgumentsToMessageSource() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        handler.setMessageSource(messageSource);

        Method method =
                GlobalExceptionHandler.class.getDeclaredMethod(
                        "buildProblemDetail",
                        Exception.class,
                        HttpStatusCode.class,
                        String.class,
                        String.class,
                        String.class,
                        Object[].class);
        method.setAccessible(true);

        Object[] args = new Object[] {"arg1"};

        ProblemDetail body =
                (ProblemDetail)
                        method.invoke(
                                handler,
                                new IllegalArgumentException("boom"),
                                HttpStatus.BAD_REQUEST,
                                "problemDetail.title.test",
                                "default detail",
                                "problemDetail.detail.test",
                                args);

        verify(messageSource)
                .getMessage(
                        eq("problemDetail.detail.test"),
                        eq(args),
                        nullable(String.class),
                        any(Locale.class));
        assertThat(body).isNotNull();
    }
}
