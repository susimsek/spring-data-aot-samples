package io.github.susimsek.springdataaotsamples.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springdataaotsamples.service.NoteRevisionService;
import io.github.susimsek.springdataaotsamples.service.NoteTrashService;
import io.github.susimsek.springdataaotsamples.service.command.NoteCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.query.NoteQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = NoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoteControllerTest {

    private static final Instant DEFAULT_TIMESTAMP = Instant.parse("2024-01-01T10:15:30Z");

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @MockitoBean private NoteCommandService noteCommandService;

    @MockitoBean private NoteQueryService noteQueryService;

    @MockitoBean private NoteRevisionService noteRevisionService;

    @MockitoBean private NoteTrashService noteTrashService;

    @Test
    void createShouldReturnCreatedNote() throws Exception {
        NoteCreateRequest request =
                new NoteCreateRequest(
                        "My first note", "Hello auditing world", true, "#2563eb", Set.of("audit"));

        NoteDTO note =
                sampleNote(
                        "My first note", "Hello auditing world", true, "#2563eb", Set.of("audit"));

        when(noteCommandService.create(any(NoteCreateRequest.class))).thenReturn(note);

        mockMvc.perform(
                        post("/api/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("My first note"))
                .andExpect(jsonPath("$.pinned").value(true));

        verify(noteCommandService).create(any(NoteCreateRequest.class));
    }

    @Test
    void findAllShouldReturnPagedNotesAndPropagateFilters() throws Exception {
        NoteDTO note =
                sampleNote(
                        "Filtered note",
                        "Filtered content",
                        false,
                        "#123456",
                        Set.of("java", "spring"));
        PageImpl<NoteDTO> page = new PageImpl<>(List.of(note), PageRequest.of(1, 5), 6);

        when(noteQueryService.findAllForCurrentUser(
                        any(Pageable.class),
                        anyString(),
                        anySet(),
                        anyString(),
                        any(Boolean.class)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/notes")
                                .param("q", "search")
                                .param("tags", "java")
                                .param("tags", "spring")
                                .param("color", "#123456")
                                .param("pinned", "true")
                                .param("page", "1")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Filtered note"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Set<String>> tagsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<String> colorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> pinnedCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(noteQueryService)
                .findAllForCurrentUser(
                        pageableCaptor.capture(),
                        queryCaptor.capture(),
                        tagsCaptor.capture(),
                        colorCaptor.capture(),
                        pinnedCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(queryCaptor.getValue()).isEqualTo("search");
        assertThat(tagsCaptor.getValue()).containsExactlyInAnyOrder("java", "spring");
        assertThat(colorCaptor.getValue()).isEqualTo("#123456");
        assertThat(pinnedCaptor.getValue()).isTrue();
    }

    @Test
    void updateShouldReturnUpdatedNote() throws Exception {
        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Updated title", "Updated content", false, "#654321", Set.of("updated"));
        NoteDTO updatedNote =
                sampleNote("Updated title", "Updated content", false, "#654321", Set.of("updated"));

        when(noteCommandService.updateForCurrentUser(anyLong(), any(NoteUpdateRequest.class)))
                .thenReturn(updatedNote);

        mockMvc.perform(
                        put("/api/notes/{id}", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.pinned").value(false));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NoteUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(NoteUpdateRequest.class);
        verify(noteCommandService)
                .updateForCurrentUser(idCaptor.capture(), requestCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(10L);
    }

    @Test
    void patchShouldReturnPatchedNote() throws Exception {
        NotePatchRequest request = new NotePatchRequest("Patched title", null, null, null, null);
        NoteDTO patchedNote =
                sampleNote(
                        "Patched title", "Hello auditing world", true, "#2563eb", Set.of("audit"));

        when(noteCommandService.patchForCurrentUser(anyLong(), any(NotePatchRequest.class)))
                .thenReturn(patchedNote);

        mockMvc.perform(
                        patch("/api/notes/{id}", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patched title"));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NotePatchRequest> requestCaptor =
                ArgumentCaptor.forClass(NotePatchRequest.class);
        verify(noteCommandService).patchForCurrentUser(idCaptor.capture(), requestCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(10L);
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notes/{id}", 7L)).andExpect(status().isNoContent());

        verify(noteCommandService).deleteForCurrentUser(7L);
    }

    @Test
    void findByIdShouldReturnNote() throws Exception {
        NoteDTO note =
                sampleNote("My note", "Hello auditing world", true, "#2563eb", Set.of("audit"));
        when(noteQueryService.findByIdForCurrentUser(9L)).thenReturn(note);

        mockMvc.perform(get("/api/notes/{id}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("My note"));

        verify(noteQueryService).findByIdForCurrentUser(9L);
    }

    @Test
    void findDeletedShouldReturnPagedNotes() throws Exception {
        NoteDTO note = sampleNote("Deleted note", "Deleted content", false, "#123456", Set.of());
        PageImpl<NoteDTO> page = new PageImpl<>(List.of(note), PageRequest.of(0, 3), 1);

        when(noteTrashService.findDeletedForCurrentUser(
                        any(Pageable.class),
                        anyString(),
                        nullable(Set.class),
                        anyString(),
                        any(Boolean.class)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/notes/deleted")
                                .param("q", "q")
                                .param("color", "#123456")
                                .param("pinned", "false")
                                .param("page", "0")
                                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Deleted note"))
                .andExpect(jsonPath("$.page.size").value(3));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Set<String>> tagsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<String> colorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> pinnedCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(noteTrashService)
                .findDeletedForCurrentUser(
                        pageableCaptor.capture(),
                        queryCaptor.capture(),
                        tagsCaptor.capture(),
                        colorCaptor.capture(),
                        pinnedCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo("q");
        assertThat(tagsCaptor.getValue()).isNull();
        assertThat(colorCaptor.getValue()).isEqualTo("#123456");
        assertThat(pinnedCaptor.getValue()).isFalse();
    }

    @Test
    void emptyTrashShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notes/deleted")).andExpect(status().isNoContent());

        verify(noteTrashService).emptyTrashForCurrentUser();
    }

    @Test
    void restoreShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/notes/{id}/restore", 4L)).andExpect(status().isNoContent());

        verify(noteTrashService).restoreForCurrentUser(4L);
    }

    @Test
    void deletePermanentlyShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notes/{id}/permanent", 4L)).andExpect(status().isNoContent());

        verify(noteTrashService).deletePermanentlyForCurrentUser(4L);
    }

    @Test
    void bulkShouldReturnResult() throws Exception {
        BulkActionRequest request = new BulkActionRequest("DELETE_SOFT", Set.of(1L, 2L));
        BulkActionResult result = new BulkActionResult(1, List.of(2L));
        when(noteCommandService.bulkForCurrentUser(any(BulkActionRequest.class)))
                .thenReturn(result);

        mockMvc.perform(
                        post("/api/notes/bulk")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.failedIds[0]").value(2));

        verify(noteCommandService).bulkForCurrentUser(any(BulkActionRequest.class));
    }

    @Test
    void findRevisionsShouldReturnPagedRevisions() throws Exception {
        NoteRevisionDTO revision =
                sampleRevision(3L, "MOD", sampleNote("t", "c", false, null, Set.of()));
        PageImpl<NoteRevisionDTO> page = new PageImpl<>(List.of(revision), PageRequest.of(0, 5), 1);

        when(noteRevisionService.findRevisionsForCurrentUser(anyLong(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/notes/{id}/revisions", 5L).param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].revision").value(3))
                .andExpect(jsonPath("$.content[0].note.id").value(1));

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteRevisionService)
                .findRevisionsForCurrentUser(idCaptor.capture(), pageableCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(5L);
    }

    @Test
    void findRevisionShouldReturnRevision() throws Exception {
        NoteRevisionDTO revision =
                sampleRevision(7L, "ADD", sampleNote("t", "c", false, null, Set.of()));
        when(noteRevisionService.findRevisionForCurrentUser(5L, 7L)).thenReturn(revision);

        mockMvc.perform(get("/api/notes/{id}/revisions/{revisionId}", 5L, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(7))
                .andExpect(jsonPath("$.revisionType").value("ADD"));

        verify(noteRevisionService).findRevisionForCurrentUser(5L, 7L);
    }

    @Test
    void restoreRevisionShouldReturnRestoredNote() throws Exception {
        NoteDTO restored = sampleNote("Restored", "From revision", false, null, Set.of());
        when(noteRevisionService.restoreRevisionForCurrentUser(5L, 7L)).thenReturn(restored);

        mockMvc.perform(post("/api/notes/{id}/revisions/{revisionId}/restore", 5L, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Restored"));

        verify(noteRevisionService).restoreRevisionForCurrentUser(5L, 7L);
    }

    private NoteDTO sampleNote(
            String title, String content, boolean pinned, String color, Set<String> tags) {
        return new NoteDTO(
                1L,
                title,
                content,
                pinned,
                color,
                "alice",
                tags,
                0L,
                "alice",
                DEFAULT_TIMESTAMP,
                "alice",
                DEFAULT_TIMESTAMP,
                false,
                null,
                null);
    }

    private NoteRevisionDTO sampleRevision(Long revision, String type, NoteDTO note) {
        return new NoteRevisionDTO(revision, type, DEFAULT_TIMESTAMP, "alice", note);
    }
}
