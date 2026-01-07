package io.github.susimsek.springdataaotsamples.web;

import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import io.github.susimsek.springdataaotsamples.service.NoteShareService;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoteShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoteShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;

    @MockitoBean private NoteShareService noteShareService;

    @Test
    void createShouldReturnCreatedShare() throws Exception {
        NoteShareDTO dto =
                new NoteShareDTO(
                        1L,
                        "raw",
                        10L,
                        SharePermission.READ,
                        Instant.now().plusSeconds(60),
                        true,
                        false,
                        0,
                        Instant.now(),
                        Instant.now(),
                        false,
                        "title",
                        "alice");
        when(noteShareService.createForCurrentUser(any(), any(CreateShareTokenRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(
                        post("/api/notes/{id}/share", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                new CreateShareTokenRequest(null, true, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("raw"));
    }

    @Test
    void listShouldReturnSharePage() throws Exception {
        Page<NoteShareDTO> page =
                new PageImpl<>(
                        List.of(
                                new NoteShareDTO(
                                        2L,
                                        "t",
                                        1L,
                                        SharePermission.READ,
                                        null,
                                        false,
                                        false,
                                        0,
                                        Instant.now(),
                                        Instant.now(),
                                        false,
                                        "title",
                                        "alice")),
                        PageRequest.of(0, 5),
                        1);
        when(noteShareService.listForCurrentUser(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/notes/{id}/share", 1L).param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2));
    }

    @Test
    void listMineShouldReturnCurrentUserShares() throws Exception {
        when(noteShareService.listAllForCurrentUser(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/notes/share").param("page", "0").param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void revokeShouldCallService() throws Exception {
        mockMvc.perform(delete("/api/notes/share/{id}", 3L)).andExpect(status().isNoContent());

        verify(noteShareService).revokeForCurrentUser(3L);
    }
}
