package io.github.susimsek.springdataaotsamples.web.admin;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.susimsek.springdataaotsamples.IntegrationTest;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.OwnerChangeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.RevisionSort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
class AdminNoteControllerIT {

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private NoteRepository noteRepository;

    @AfterEach
    void cleanup() {
        noteRepository.deleteAll();
    }

    @Test
    void createShouldReturnCreatedNote() throws Exception {
        NoteCreateRequest request =
                new NoteCreateRequest(
                        "Admin note", "Content for admin note", true, "#2563eb", Set.of());

        mockMvc.perform(
                        post("/api/admin/notes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Admin note"))
                .andExpect(jsonPath("$.owner").value("admin"))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void findAllShouldReturnAllNotes() throws Exception {
        createNote("Admin list A", "alice");
        createNote("Admin list B", "bob");

        mockMvc.perform(get("/api/admin/notes").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(
                        jsonPath("$.content[*].owner")
                                .value(containsInAnyOrder("alice", "bob")))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void findByIdShouldReturnNote() throws Exception {
        Note note = createNote("Admin find note", "alice");

        mockMvc.perform(get("/api/admin/notes/{id}", note.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin find note"));
    }

    @Test
    void updateShouldReturnUpdatedNote() throws Exception {
        Note note = createNote("Admin old title", "alice");
        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Admin updated title",
                        "Updated content",
                        false,
                        "#123456",
                        Set.of());

        mockMvc.perform(
                        put("/api/admin/notes/{id}", note.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin updated title"))
                .andExpect(jsonPath("$.pinned").value(false));
    }

    @Test
    void patchShouldReturnPatchedNote() throws Exception {
        Note note = createNote("Admin original", "alice");
        NotePatchRequest request =
                new NotePatchRequest("Admin patched", "Partial content", null, null, null);

        mockMvc.perform(
                        patch("/api/admin/notes/{id}", note.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin patched"))
                .andExpect(jsonPath("$.content").value("Partial content"));
    }

    @Test
    void deleteShouldSoftDeleteNote() throws Exception {
        Note note = createNote("Admin delete note", "alice");

        mockMvc.perform(delete("/api/admin/notes/{id}", note.getId()))
                .andExpect(status().isNoContent());

        Note deleted = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    void findDeletedShouldReturnPagedNotes() throws Exception {
        Note note = createDeletedNote("Admin deleted note", "alice");

        mockMvc.perform(get("/api/admin/notes/deleted").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(note.getTitle()))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void restoreShouldReturnNoContent() throws Exception {
        Note note = createDeletedNote("Admin restore note", "alice");

        mockMvc.perform(post("/api/admin/notes/{id}/restore", note.getId()))
                .andExpect(status().isNoContent());

        Note restored = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(restored.isDeleted()).isFalse();
    }

    @Test
    void deletePermanentlyShouldRemoveNote() throws Exception {
        Note note = createDeletedNote("Admin permanent note", "alice");

        mockMvc.perform(delete("/api/admin/notes/{id}/permanent", note.getId()))
                .andExpect(status().isNoContent());

        assertThat(noteRepository.findById(note.getId())).isEmpty();
    }

    @Test
    void emptyTrashShouldRemoveDeletedNotes() throws Exception {
        createDeletedNote("Admin trash", "alice");

        mockMvc.perform(delete("/api/admin/notes/deleted")).andExpect(status().isNoContent());

        boolean hasDeleted = noteRepository.findAll().stream().anyMatch(Note::isDeleted);
        assertThat(hasDeleted).isFalse();
    }

    @Test
    void bulkShouldReturnProcessedAndFailed() throws Exception {
        Note active = createNote("Admin bulk active", "alice");
        Note alreadyDeleted = createDeletedNote("Admin bulk deleted", "bob");
        BulkActionRequest request =
                new BulkActionRequest(
                        "DELETE_SOFT", Set.of(active.getId(), alreadyDeleted.getId()));

        mockMvc.perform(
                        post("/api/admin/notes/bulk")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.failedIds[0]").value(alreadyDeleted.getId()));
    }

    @Test
    void findRevisionsShouldReturnPagedRevisions() throws Exception {
        Note note = createNoteWithRevisions();

        mockMvc.perform(
                        get("/api/admin/notes/{id}/revisions", note.getId())
                                .param("page", "0")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].revision").isNotEmpty());
    }

    @Test
    void findRevisionShouldReturnRevision() throws Exception {
        Note note = createNoteWithRevisions();
        Long revisionId = latestRevisionId(note.getId());

        mockMvc.perform(get("/api/admin/notes/{id}/revisions/{rev}", note.getId(), revisionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(revisionId));
    }

    @Test
    void restoreRevisionShouldReturnRestoredNote() throws Exception {
        Note note = createNoteWithRevisions();
        String originalTitle = "Admin revision note";
        Long oldestRevisionId = oldestRevisionId(note.getId());

        mockMvc.perform(
                        post(
                                        "/api/admin/notes/{id}/revisions/{rev}/restore",
                                        note.getId(),
                                        oldestRevisionId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(originalTitle));
    }

    @Test
    void changeOwnerShouldUpdateOwnerWhenUserExists() throws Exception {
        Note note = createNote("Admin change owner", "alice");
        OwnerChangeRequest request = new OwnerChangeRequest("user1");

        mockMvc.perform(
                        post("/api/admin/notes/{id}/owner", note.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("user1"));
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
        Note note = createNote("Admin revision note", "alice");
        note.setTitle("Admin revision note v2");
        note.setContent("Updated revision content");
        noteRepository.saveAndFlush(note);
        return note;
    }

    private Long latestRevisionId(Long noteId) {
        var revisions =
                noteRepository.findRevisions(noteId, PageRequest.of(0, 10, RevisionSort.desc()));
        return revisions.getContent().getFirst().getMetadata().getRevisionNumber().orElseThrow();
    }

    private Long oldestRevisionId(Long noteId) {
        var revisions =
                noteRepository.findRevisions(noteId, PageRequest.of(0, 10, RevisionSort.asc()));
        var content = revisions.getContent();
        return content.getFirst().getMetadata().getRevisionNumber().orElseThrow();
    }

}
