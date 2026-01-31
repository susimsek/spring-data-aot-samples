package io.github.susimsek.springdataaotsamples.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import io.github.susimsek.springdataaotsamples.service.NoteShareService;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = NoteAdminShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoteAdminShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;

    @MockitoBean private NoteShareService noteShareService;

    @Test
    void createShouldReturnCreatedShare() throws Exception {
        NoteShareDTO dto =
                new NoteShareDTO(
                        10L,
                        "raw",
                        5L,
                        SharePermission.READ,
                        null,
                        false,
                        false,
                        0,
                        Instant.now(),
                        Instant.now(),
                        false,
                        "title",
                        "alice");
        when(noteShareService.create(any(), any(CreateShareTokenRequest.class))).thenReturn(dto);

        mockMvc.perform(
                        post("/api/admin/notes/{id}/share", 5L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                new CreateShareTokenRequest(null, false, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void listShouldReturnAdminShares() throws Exception {
        Page<NoteShareDTO> page =
                new PageImpl<>(
                        List.of(
                                new NoteShareDTO(
                                        1L,
                                        "t",
                                        5L,
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
        when(noteShareService.listForAdmin(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/notes/{id}/share", 5L)
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].noteId").value(5));
    }

    @Test
    void listAllShouldReturnAdminSharePage() throws Exception {
        when(noteShareService.listAllForAdmin(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/notes/share").param("page", "0").param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void revokeShouldInvokeService() throws Exception {
        mockMvc.perform(delete("/api/admin/notes/share/{id}", 2L))
                .andExpect(status().isNoContent());
        verify(noteShareService).revoke(2L);
    }
}
