package io.github.susimsek.springdataaotsamples.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.service.NoteShareService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShareApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShareApiControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NoteShareService noteShareService;
    @MockitoBean private NoteMapper noteMapper;

    @Test
    void getSharedShouldReturnMappedNote() throws Exception {
        NoteShareToken token = new NoteShareToken();
        Note note = new Note();
        note.setId(5L);
        note.setTitle("Shared");
        token.setNote(note);
        when(noteShareService.validateAndConsume("abc")).thenReturn(token);
        when(noteMapper.toDto(note))
                .thenReturn(
                        new NoteDTO(
                                5L,
                                "Shared",
                                "c",
                                false,
                                null,
                                "alice",
                                Set.of(),
                                0L,
                                "alice",
                                Instant.now(),
                                "alice",
                                Instant.now(),
                                false,
                                null,
                                null));

        mockMvc.perform(get("/api/share/{token}", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Shared"))
                .andExpect(jsonPath("$.id").value(5));
    }
}
