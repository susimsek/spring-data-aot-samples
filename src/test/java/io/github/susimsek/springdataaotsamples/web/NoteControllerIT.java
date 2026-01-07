package io.github.susimsek.springdataaotsamples.web;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.susimsek.springdataaotsamples.IntegrationTest;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.RevisionSort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "alice")
class NoteControllerIT {

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private NoteRepository noteRepository;

    @AfterEach
    void clearSecurityContext() {
        noteRepository.deleteAll();
    }

    @Test
    void createShouldReturnCreatedNote() throws Exception {
        NoteCreateRequest request =
                new NoteCreateRequest(
                        "My first note",
                        "Hello auditing world",
                        true,
                        "#2563eb",
                        Set.of());

        mockMvc.perform(
                        post("/api/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My first note"))
                .andExpect(jsonPath("$.pinned").value(true));

        List<Note> stored = noteRepository.findAll();
        assertThat(stored).anySatisfy(note -> {
            assertThat(note.getTitle()).isEqualTo("My first note");
            assertThat(note.getOwner()).isEqualTo("alice");
        });
    }

    @Test
    void findAllShouldReturnOnlyCurrentUsersNotes() throws Exception {
        createNote("Alice note", "alice");
        createNote("Bob note", "bob");

        mockMvc.perform(get("/api/notes").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].owner").value("alice"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void findByIdShouldReturnNote() throws Exception {
        Note note = createNote("Find note", "alice");

        mockMvc.perform(get("/api/notes/{id}", note.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Find note"));
    }

    @Test
    void updateShouldReturnUpdatedNote() throws Exception {
        Note note = createNote("Old title", "alice");
        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Updated title",
                        "Updated content",
                        false,
                        "#123456",
                        Set.of());

        mockMvc.perform(
                        put("/api/notes/{id}", note.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.pinned").value(false));
    }

    @Test
    void patchShouldReturnPatchedNote() throws Exception {
        Note note = createNote("Original title", "alice");
        NotePatchRequest request = new NotePatchRequest("Patched title", null, null, null, null);

        mockMvc.perform(
                        patch("/api/notes/{id}", note.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patched title"));
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        Note note = createNote("Delete note", "alice");

        mockMvc.perform(delete("/api/notes/{id}", note.getId()))
                .andExpect(status().isNoContent());

        Note deleted = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    void findDeletedShouldReturnPagedNotes() throws Exception {
        Note note = createDeletedNote("Deleted note", "alice");

        mockMvc.perform(get("/api/notes/deleted").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(note.getTitle()))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void restoreShouldReturnNoContent() throws Exception {
        Note note = createDeletedNote("Restore note", "alice");

        mockMvc.perform(post("/api/notes/{id}/restore", note.getId()))
                .andExpect(status().isNoContent());

        Note restored = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(restored.isDeleted()).isFalse();
    }

    @Test
    void deletePermanentlyShouldReturnNoContent() throws Exception {
        Note note = createDeletedNote("Permanent note", "alice");

        mockMvc.perform(delete("/api/notes/{id}/permanent", note.getId()))
                .andExpect(status().isNoContent());

        assertThat(noteRepository.findById(note.getId())).isEmpty();
    }

    @Test
    void emptyTrashShouldReturnNoContent() throws Exception {
        createDeletedNote("Trash note", "alice");

        mockMvc.perform(delete("/api/notes/deleted")).andExpect(status().isNoContent());

        boolean hasDeleted =
                noteRepository.findAll().stream()
                        .anyMatch(note -> note.isDeleted() && "alice".equals(note.getOwner()));
        assertThat(hasDeleted).isFalse();
    }

    @Test
    void bulkShouldReturnProcessedAndFailed() throws Exception {
        Note aliceNote = createNote("Bulk alice", "alice");
        Note bobNote = createNote("Bulk bob", "bob");

        BulkActionRequest request =
                new BulkActionRequest(
                        "DELETE_SOFT", Set.of(aliceNote.getId(), bobNote.getId()));

        mockMvc.perform(
                        post("/api/notes/bulk")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.failedIds[0]").value(bobNote.getId()));
    }

    @Test
    void findRevisionsShouldReturnPagedRevisions() throws Exception {
        Note note = createNoteWithRevisions();

        mockMvc.perform(
                        get("/api/notes/{id}/revisions", note.getId())
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].revision").isNotEmpty());
    }

    @Test
    void findRevisionShouldReturnRevision() throws Exception {
        Note note = createNoteWithRevisions();
        Long revisionId = latestRevisionId(note.getId());

        mockMvc.perform(get("/api/notes/{id}/revisions/{rev}", note.getId(), revisionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(revisionId));
    }

    @Test
    void restoreRevisionShouldReturnRestoredNote() throws Exception {
        Note note = createNoteWithRevisions();
        String originalTitle = "Revision note";
        Long oldestRevisionId = oldestRevisionId(note.getId());

        mockMvc.perform(
                        post(
                                        "/api/notes/{id}/revisions/{rev}/restore",
                                        note.getId(),
                                        oldestRevisionId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(originalTitle));
    }

    private Note createNote(String title, String owner) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(title + " content");
        note.setOwner(owner);
        note.setPinned(false);
        note.setColor("#123456");
        note.setVersion(0L);
        return noteRepository.save(note);
    }

    private Note createDeletedNote(String title, String owner) {
        Note note = createNote(title, owner);
        noteRepository.softDeleteById(note.getId());
        return noteRepository.findById(note.getId()).orElseThrow();
    }

    private Note createNoteWithRevisions() {
        Note note = createNote("Revision note", "alice");
        note.setTitle("Revision note v2");
        note.setContent("Updated revision content");
        noteRepository.saveAndFlush(note);
        return note;
    }

    private Long latestRevisionId(Long noteId) {
        var revisions =
                noteRepository.findRevisions(noteId, PageRequest.of(0, 10, RevisionSort.desc()));
        return revisions.getContent().get(0).getMetadata().getRevisionNumber().orElseThrow();
    }

    private Long oldestRevisionId(Long noteId) {
        var revisions =
                noteRepository.findRevisions(noteId, PageRequest.of(0, 10, RevisionSort.asc()));
        var content = revisions.getContent();
        return content.get(0).getMetadata().getRevisionNumber().orElseThrow();
    }
}
