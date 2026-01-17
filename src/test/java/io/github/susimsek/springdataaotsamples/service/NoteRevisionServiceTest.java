package io.github.susimsek.springdataaotsamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.RevisionNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteRevisionMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.RevisionSort;

@ExtendWith(MockitoExtension.class)
class NoteRevisionServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private NoteRevisionMapper noteRevisionMapper;
    @Mock private NoteAuthorizationService noteAuthorizationService;
    @Mock private NoteMapper noteMapper;

    @InjectMocks private NoteRevisionService noteRevisionService;

    @Test
    void findRevisionsShouldMapToDtoAndUseDescendingSort() {
        Note note = new Note();
        note.setId(1L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        Revision<Long, Note> rev = Revision.of(mockMeta(1L), note);
        Page<Revision<Long, Note>> page = new PageImpl<>(List.of(rev));
        when(noteRepository.findRevisions(
                        1L, PageRequest.of(0, 5, RevisionSort.desc())))
                .thenReturn(page);
        NoteRevisionDTO dto =
                new NoteRevisionDTO(1L, "ADD", Instant.now(), "alice", sampleDto("a"));
        when(noteRevisionMapper.toRevisionDto(rev)).thenReturn(dto);

        Page<NoteRevisionDTO> result = noteRevisionService.findRevisions(1L, PageRequest.of(0, 5));

        assertThat(result.getContent()).singleElement().isEqualTo(dto);
    }

    @Test
    void findRevisionShouldThrowWhenMissing() {
        when(noteRepository.findRevision(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteRevisionService.findRevision(1L, 2L))
                .isInstanceOf(RevisionNotFoundException.class);
    }

    @Test
    void findRevisionShouldReturnDto() {
        Note note = new Note();
        Revision<Long, Note> rev = Revision.of(mockMeta(2L), note);
        when(noteRepository.findRevision(1L, 2L)).thenReturn(Optional.of(rev));
        NoteRevisionDTO dto = new NoteRevisionDTO(2L, "MOD", Instant.now(), "bob", sampleDto("b"));
        when(noteRevisionMapper.toRevisionDto(rev)).thenReturn(dto);

        NoteRevisionDTO result = noteRevisionService.findRevision(1L, 2L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void restoreRevisionShouldThrowWhenNoteMissing() {
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteRevisionService.restoreRevision(1L, 2L))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void restoreRevisionShouldApplySnapshotAndSave() {
        Note note = new Note();
        Note snapshot = new Note();
        snapshot.setTitle("snapshot");
        Revision<Long, Note> rev = Revision.of(mockMeta(3L), snapshot);
        NoteDTO dto = sampleDto("snapshot");
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.findRevision(1L, 3L)).thenReturn(Optional.of(rev));
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteRevisionService.restoreRevision(1L, 3L);

        assertThat(result).isEqualTo(dto);
        verify(noteMapper).applyRevision(snapshot, note);
        verify(noteRepository).save(note);
    }

    @Test
    void restoreRevisionForCurrentUserShouldCheckAccess() {
        Note note = new Note();
        Note snapshot = new Note();
        Revision<Long, Note> rev = Revision.of(mockMeta(4L), snapshot);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.findRevision(1L, 4L)).thenReturn(Optional.of(rev));
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(sampleDto("restored"));

        noteRevisionService.restoreRevisionForCurrentUser(1L, 4L);

        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(noteMapper).applyRevision(snapshot, note);
    }

    @Test
    void findRevisionsForCurrentUserShouldCheckReadAccess() {
        Note note = new Note();
        note.setId(5L);
        when(noteRepository.findById(5L)).thenReturn(Optional.of(note));
        Revision<Long, Note> rev = Revision.of(mockMeta(6L), note);
        when(noteRepository.findRevisions(
                        5L, PageRequest.of(0, 3, RevisionSort.desc())))
                .thenReturn(new PageImpl<>(List.of(rev)));
        NoteRevisionDTO dto =
                new NoteRevisionDTO(6L, "MOD", Instant.now(), "alice", sampleDto("t"));
        when(noteRevisionMapper.toRevisionDto(rev)).thenReturn(dto);

        Page<NoteRevisionDTO> result =
                noteRevisionService.findRevisionsForCurrentUser(5L, PageRequest.of(0, 3));

        verify(noteAuthorizationService).ensureReadAccess(note);
        assertThat(result.getContent()).singleElement().isEqualTo(dto);
    }

    @Test
    void findRevisionForCurrentUserShouldThrowWhenNoteMissing() {
        when(noteRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteRevisionService.findRevisionForCurrentUser(7L, 1L))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void findRevisionForCurrentUserShouldCheckAccessAndReturnDto() {
        Note note = new Note();
        when(noteRepository.findById(8L)).thenReturn(Optional.of(note));
        Revision<Long, Note> rev = Revision.of(mockMeta(9L), note);
        when(noteRepository.findRevision(8L, 9L)).thenReturn(Optional.of(rev));
        NoteRevisionDTO dto = new NoteRevisionDTO(9L, "ADD", Instant.now(), "bob", sampleDto("t3"));
        when(noteRevisionMapper.toRevisionDto(rev)).thenReturn(dto);

        NoteRevisionDTO result = noteRevisionService.findRevisionForCurrentUser(8L, 9L);

        verify(noteAuthorizationService).ensureReadAccess(note);
        assertThat(result).isEqualTo(dto);
    }

    private RevisionMetadata<Long> mockMeta(Long id) {
        return new RevisionMetadata<>() {
            @Override
            public Optional<Long> getRevisionNumber() {
                return Optional.ofNullable(id);
            }

            @Override
            public Optional<Instant> getRevisionInstant() {
                return Optional.of(Instant.now());
            }

            @Override
            public <T> T getDelegate() {
                return null;
            }

            @Override
            public RevisionType getRevisionType() {
                return RevisionType.INSERT;
            }
        };
    }

    private NoteDTO sampleDto(String title) {
        return new NoteDTO(
                1L,
                title,
                "content",
                false,
                "#123",
                "alice",
                Set.of(),
                0L,
                "alice",
                Instant.now(),
                "alice",
                Instant.now(),
                false,
                null,
                null);
    }
}
