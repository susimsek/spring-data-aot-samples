package io.github.susimsek.springdataaotsamples.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springdataaotsamples.security.AuthenticationService;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
import io.github.susimsek.springdataaotsamples.security.CookieUtils;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.dto.LoginRequest;
import io.github.susimsek.springdataaotsamples.service.dto.LogoutRequest;
import io.github.susimsek.springdataaotsamples.service.dto.TokenDTO;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @MockitoBean private AuthenticationService authenticationService;

    @Test
    void loginShouldReturnTokenAndSetCookies() throws Exception {
        TokenDTO token =
                new TokenDTO(
                        "jwt",
                        "Bearer",
                        Instant.now().plusSeconds(60),
                        "refresh",
                        Instant.now().plusSeconds(120),
                        "alice",
                        Set.of(AuthoritiesConstants.USER));
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(token);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                new LoginRequest("alice", "secret", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.SET_COOKIE,
                                        hasItems(
                                                containsString("AUTH-TOKEN"),
                                                containsString("REFRESH-TOKEN"))));
    }

    @Test
    @WithMockUser
    void meShouldReturnCurrentUser() throws Exception {
        UserDTO dto = new UserDTO(1L, "alice", "alice@example.com", Set.of(AuthoritiesConstants.USER));
        when(authenticationService.getCurrentUser()).thenReturn(dto);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void logoutShouldUseBodyRefreshTokenIfProvidedElseCookie() throws Exception {
        doNothing().when(authenticationService).logout("bodyRefresh");

        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                new LogoutRequest(
                                                        "bodyRefresh-token-value-123456"))))
                .andExpect(status().isNoContent())
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.SET_COOKIE,
                                        hasItems(
                                                containsString("AUTH-TOKEN"),
                                                containsString("REFRESH-TOKEN"))));

        verify(authenticationService).logout("bodyRefresh-token-value-123456");
    }

    @Test
    void refreshShouldFallbackToCookieWhenBodyMissing() throws Exception {
        TokenDTO token =
                new TokenDTO(
                        "newJwt",
                        "Bearer",
                        Instant.now().plusSeconds(60),
                        "newRefresh",
                        Instant.now().plusSeconds(120),
                        "alice",
                        Set.of(AuthoritiesConstants.USER));
        when(authenticationService.refresh("cookieRefresh")).thenReturn(token);

        try (MockedStatic<CookieUtils> cookies =
                Mockito.mockStatic(CookieUtils.class, Mockito.CALLS_REAL_METHODS)) {
            cookies.when(
                            () ->
                                    CookieUtils.getCookieValue(
                                            any(), Mockito.eq(SecurityUtils.REFRESH_COOKIE)))
                    .thenReturn("cookieRefresh");

            mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("newJwt"))
                    .andExpect(
                            header().stringValues(
                                            HttpHeaders.SET_COOKIE,
                                            hasItems(
                                                    containsString("AUTH-TOKEN"),
                                                    containsString("REFRESH-TOKEN"))));
        }
    }
}
