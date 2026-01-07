package io.github.susimsek.springdataaotsamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import io.github.susimsek.springdataaotsamples.service.dto.OwnerChangeRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(controllers = AdminNoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminNoteControllerTest {

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
                        "Admin note", "Hello auditing world", true, "#2563eb", Set.of("audit"));
        NoteDTO note =
                sampleNote("Admin note", "Hello auditing world", true, "#2563eb", Set.of("audit"));

        when(noteCommandService.create(any(NoteCreateRequest.class))).thenReturn(note);

        mockMvc.perform(
                        post("/api/admin/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner").value("alice"))
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

        when(noteQueryService.findAll(
                        any(Pageable.class), eq("search"), anySet(), eq("#123456"), eq(true)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/notes")
                                .param("q", "search")
                                .param("tags", "java")
                                .param("tags", "spring")
                                .param("color", "#123456")
                                .param("pinned", "true")
                                .param("page", "1")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Filtered note"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Set<String>> tagsCaptor = ArgumentCaptor.forClass(Set.class);

        verify(noteQueryService)
                .findAll(
                        pageableCaptor.capture(),
                        eq("search"),
                        tagsCaptor.capture(),
                        eq("#123456"),
                        eq(true));

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(tagsCaptor.getValue()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void updateShouldReturnUpdatedNote() throws Exception {
        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Updated title", "Updated content", false, "#654321", Set.of("updated"));
        NoteDTO updatedNote =
                sampleNote("Updated title", "Updated content", false, "#654321", Set.of("updated"));

        when(noteCommandService.update(eq(10L), any(NoteUpdateRequest.class)))
                .thenReturn(updatedNote);

        mockMvc.perform(
                        put("/api/admin/notes/{id}", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(noteCommandService).update(eq(10L), any(NoteUpdateRequest.class));
    }

    @Test
    void patchShouldReturnPatchedNote() throws Exception {
        NotePatchRequest request = new NotePatchRequest("Patched title", null, null, null, null);
        NoteDTO patchedNote =
                sampleNote(
                        "Patched title", "Hello auditing world", true, "#2563eb", Set.of("audit"));

        when(noteCommandService.patch(eq(10L), any(NotePatchRequest.class)))
                .thenReturn(patchedNote);

        mockMvc.perform(
                        patch("/api/admin/notes/{id}", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patched title"));

        verify(noteCommandService).patch(eq(10L), any(NotePatchRequest.class));
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/notes/{id}", 7L)).andExpect(status().isNoContent());

        verify(noteCommandService).delete(7L);
    }

    @Test
    void findByIdShouldReturnNote() throws Exception {
        NoteDTO note =
                sampleNote("My note", "Hello auditing world", true, "#2563eb", Set.of("audit"));
        when(noteQueryService.findById(9L)).thenReturn(note);

        mockMvc.perform(get("/api/admin/notes/{id}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("My note"));

        verify(noteQueryService).findById(9L);
    }

    @Test
    void findDeletedShouldReturnPagedNotes() throws Exception {
        NoteDTO note = sampleNote("Deleted note", "Deleted content", false, "#123456", Set.of());
        PageImpl<NoteDTO> page = new PageImpl<>(List.of(note), PageRequest.of(0, 3), 1);

        when(noteTrashService.findDeleted(
                        any(Pageable.class), eq("q"), any(), eq("#123456"), eq(false)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/notes/deleted")
                                .param("q", "q")
                                .param("color", "#123456")
                                .param("pinned", "false")
                                .param("page", "0")
                                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Deleted note"))
                .andExpect(jsonPath("$.size").value(3));

        verify(noteTrashService)
                .findDeleted(any(Pageable.class), eq("q"), any(), eq("#123456"), eq(false));
    }

    @Test
    void emptyTrashShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/notes/deleted")).andExpect(status().isNoContent());

        verify(noteTrashService).emptyTrash();
    }

    @Test
    void restoreShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/admin/notes/{id}/restore", 4L))
                .andExpect(status().isNoContent());

        verify(noteTrashService).restore(4L);
    }

    @Test
    void deletePermanentlyShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/notes/{id}/permanent", 4L))
                .andExpect(status().isNoContent());

        verify(noteTrashService).deletePermanently(4L);
    }

    @Test
    void bulkShouldReturnResult() throws Exception {
        BulkActionRequest request = new BulkActionRequest("DELETE_SOFT", Set.of(1L, 2L));
        BulkActionResult result = new BulkActionResult(1, List.of(2L));
        when(noteCommandService.bulk(any(BulkActionRequest.class))).thenReturn(result);

        mockMvc.perform(
                        post("/api/admin/notes/bulk")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.failedIds[0]").value(2));

        verify(noteCommandService).bulk(any(BulkActionRequest.class));
    }

    @Test
    void findRevisionsShouldReturnPagedRevisions() throws Exception {
        NoteRevisionDTO revision =
                sampleRevision(3L, "MOD", sampleNote("t", "c", false, null, Set.of()));
        PageImpl<NoteRevisionDTO> page = new PageImpl<>(List.of(revision), PageRequest.of(0, 5), 1);

        when(noteRevisionService.findRevisions(eq(5L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/notes/{id}/revisions", 5L)
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].revision").value(3))
                .andExpect(jsonPath("$.content[0].note.id").value(1));

        verify(noteRevisionService).findRevisions(eq(5L), any(Pageable.class));
    }

    @Test
    void findRevisionShouldReturnRevision() throws Exception {
        NoteRevisionDTO revision =
                sampleRevision(7L, "ADD", sampleNote("t", "c", false, null, Set.of()));
        when(noteRevisionService.findRevision(5L, 7L)).thenReturn(revision);

        mockMvc.perform(get("/api/admin/notes/{id}/revisions/{revisionId}", 5L, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(7))
                .andExpect(jsonPath("$.revisionType").value("ADD"));

        verify(noteRevisionService).findRevision(5L, 7L);
    }

    @Test
    void restoreRevisionShouldReturnRestoredNote() throws Exception {
        NoteDTO restored = sampleNote("Restored", "From revision", false, null, Set.of());
        when(noteRevisionService.restoreRevision(5L, 7L)).thenReturn(restored);

        mockMvc.perform(post("/api/admin/notes/{id}/revisions/{revisionId}/restore", 5L, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Restored"));

        verify(noteRevisionService).restoreRevision(5L, 7L);
    }

    @Test
    void changeOwnerShouldDelegateToCommandService() throws Exception {
        OwnerChangeRequest request = new OwnerChangeRequest("new-owner");
        NoteDTO updated =
                new NoteDTO(
                        1L,
                        "Title",
                        "Content",
                        false,
                        "#123456",
                        "new-owner",
                        Set.of(),
                        0L,
                        "alice",
                        DEFAULT_TIMESTAMP,
                        "alice",
                        DEFAULT_TIMESTAMP,
                        false,
                        null,
                        null);
        when(noteCommandService.changeOwner(5L, "new-owner")).thenReturn(updated);

        mockMvc.perform(
                        post("/api/admin/notes/{id}/owner", 5L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("new-owner"));

        verify(noteCommandService).changeOwner(5L, "new-owner");
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
