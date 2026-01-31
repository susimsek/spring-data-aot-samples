package io.github.susimsek.springdataaotsamples.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springdataaotsamples.config.TestWebMvcConfig;
import io.github.susimsek.springdataaotsamples.service.dto.UserSearchDTO;
import io.github.susimsek.springdataaotsamples.service.query.UserQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestWebMvcConfig.class)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserQueryService userQueryService;

    @Test
    void searchShouldDelegateToService() throws Exception {
        Page<UserSearchDTO> page =
                new PageImpl<>(
                        List.of(new UserSearchDTO(1L, "alice", true)), PageRequest.of(0, 5), 1);
        when(userQueryService.searchUsernames(any(), any())).thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .param("q", "ali")
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"));

        verify(userQueryService).searchUsernames(any(), any());
    }
}
