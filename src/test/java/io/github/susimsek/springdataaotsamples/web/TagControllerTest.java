package io.github.susimsek.springdataaotsamples.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springdataaotsamples.service.dto.TagDTO;
import io.github.susimsek.springdataaotsamples.service.query.TagQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TagController.class)
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TagQueryService tagQueryService;

    @Test
    void suggestShouldReturnEmptyWhenQueryTooShort() throws Exception {
        mockMvc.perform(
                        get("/api/tags/suggest")
                                .param("q", "a")
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verifyNoInteractions(tagQueryService);
    }

    @Test
    void suggestShouldDelegateWhenValidQuery() throws Exception {
        Page<TagDTO> page =
                new PageImpl<>(List.of(new TagDTO(1L, "java")), PageRequest.of(0, 5), 1);
        when(tagQueryService.suggestPrefixPage("ja", PageRequest.of(0, 5))).thenReturn(page);

        mockMvc.perform(
                        get("/api/tags/suggest")
                                .param("q", " ja ")
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("java"));
    }
}
