package io.github.susimsek.springdataaotsamples.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.enumeration.BulkAction;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.NoteAuthorizationService;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.UserNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class NoteCommandServiceTest {

    private static final Instant DEFAULT_TIMESTAMP = Instant.parse("2024-01-01T10:15:30Z");

    @Mock private NoteRepository noteRepository;

    @Mock private TagCommandService tagCommandService;

    @Mock private NoteMapper noteMapper;

    @Mock private UserRepository userRepository;

    @Mock private NoteAuthorizationService noteAuthorizationService;

    @Mock private CacheProvider cacheProvider;

    @InjectMocks private NoteCommandService noteCommandService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldSetOwnerResolveTagsSaveAndReturnDto() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a"));

        NoteCreateRequest request =
                new NoteCreateRequest(
                        "My first note", "Hello auditing world", true, "#2563eb", Set.of("audit"));

        Note noteEntity = new Note();
        NoteDTO dto =
                sampleDto(
                        "My first note", "Hello auditing world", true, "#2563eb", Set.of("audit"));
        Set<Tag> resolvedTags = Set.of(new Tag(1L, "audit"));

        when(noteMapper.toEntity(request)).thenReturn(noteEntity);
        when(tagCommandService.resolveTags(request.tags())).thenReturn(resolvedTags);
        when(noteRepository.save(noteEntity)).thenReturn(noteEntity);
        when(noteMapper.toDto(noteEntity)).thenReturn(dto);

        NoteDTO result = noteCommandService.create(request);

        assertThat(result).isEqualTo(dto);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getOwner()).isEqualTo("alice");
        assertThat(noteCaptor.getValue().getTags()).isEqualTo(resolvedTags);
    }

    @Test
    void createShouldThrowWhenCurrentUserMissing() {
        NoteCreateRequest request =
                new NoteCreateRequest(
                        "My first note", "Hello auditing world", true, "#2563eb", Set.of("audit"));
        when(noteMapper.toEntity(request)).thenReturn(new Note());

        assertThrows(UsernameNotFoundException.class, () -> noteCommandService.create(request));

        verifyNoInteractions(noteRepository);
        verify(tagCommandService, never()).resolveTags(any());
    }

    @Test
    void patchForCurrentUserShouldAuthorizeAndSkipTagsWhenNull() {
        Note note = new Note();
        note.setId(1L);
        NotePatchRequest request = new NotePatchRequest("Patched title", null, null, null, null);
        NoteDTO dto = sampleDto("Patched title", "c", false, null, Set.of());

        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteCommandService.patchForCurrentUser(1L, request);

        assertThat(result).isEqualTo(dto);
        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(noteMapper).patchEntity(request, note);
        verify(tagCommandService, never()).resolveTags(any());
    }

    @Test
    void updateForCurrentUserShouldAuthorizeApplyTagsSaveAndReturnDto() {
        Note note = new Note();
        note.setId(1L);

        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Updated title", "Updated content", false, "#654321", Set.of("t1"));
        NoteDTO dto = sampleDto("Updated title", "Updated content", false, "#654321", Set.of("t1"));
        Set<Tag> resolvedTags = Set.of(new Tag(1L, "t1"));

        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(note));
        when(tagCommandService.resolveTags(request.tags())).thenReturn(resolvedTags);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteCommandService.updateForCurrentUser(1L, request);

        assertThat(result).isEqualTo(dto);
        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(noteMapper).updateEntity(request, note);
        assertThat(note.getTags()).isEqualTo(resolvedTags);
    }

    @Test
    void updateShouldApplyTagsSaveAndReturnDto() {
        Note note = new Note();
        note.setId(1L);

        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Updated title", "Updated content", true, "#654321", Set.of("t1"));
        NoteDTO dto = sampleDto("Updated title", "Updated content", true, "#654321", Set.of("t1"));
        Set<Tag> resolvedTags = Set.of(new Tag(1L, "t1"));

        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(note));
        when(tagCommandService.resolveTags(request.tags())).thenReturn(resolvedTags);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteCommandService.update(1L, request);

        assertThat(result).isEqualTo(dto);
        verify(noteMapper).updateEntity(request, note);
        assertThat(note.getTags()).isEqualTo(resolvedTags);
    }

    @Test
    void updateShouldThrowWhenNoteMissing() {
        NoteUpdateRequest request =
                new NoteUpdateRequest(
                        "Updated title", "Updated content", true, "#654321", Set.of("t1"));
        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteCommandService.update(1L, request));
    }

    @Test
    void patchShouldResolveTagsWhenProvided() {
        Note note = new Note();
        note.setId(1L);
        NotePatchRequest request = new NotePatchRequest(null, null, null, null, Set.of("a", "b"));
        Set<Tag> resolvedTags = Set.of(new Tag(1L, "a"), new Tag(2L, "b"));
        NoteDTO dto = sampleDto("t", "c", false, null, Set.of("a", "b"));

        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(note));
        when(tagCommandService.resolveTags(request.tags())).thenReturn(resolvedTags);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteCommandService.patch(1L, request);

        assertThat(result).isEqualTo(dto);
        verify(tagCommandService).resolveTags(request.tags());
        assertThat(note.getTags()).isEqualTo(resolvedTags);
    }

    @Test
    void patchShouldThrowWhenNoteMissing() {
        NotePatchRequest request = new NotePatchRequest("t", null, null, null, null);
        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteCommandService.patch(1L, request));
    }

    @Test
    void patchForCurrentUserShouldAuthorizeAndResolveTagsWhenProvided() {
        Note note = new Note();
        note.setId(1L);
        NotePatchRequest request = new NotePatchRequest(null, null, null, null, Set.of("a"));
        Set<Tag> resolvedTags = Set.of(new Tag(1L, "a"));
        NoteDTO dto = sampleDto("t", "c", false, null, Set.of("a"));

        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(note));
        when(tagCommandService.resolveTags(request.tags())).thenReturn(resolvedTags);
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteCommandService.patchForCurrentUser(1L, request);

        assertThat(result).isEqualTo(dto);
        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(tagCommandService).resolveTags(request.tags());
        assertThat(note.getTags()).isEqualTo(resolvedTags);
    }

    @Test
    void deleteShouldThrowWhenSoftDeleteFails() {
        when(noteRepository.softDeleteById(7L)).thenReturn(0);

        assertThrows(NoteNotFoundException.class, () -> noteCommandService.delete(7L));

        verify(cacheProvider, never()).clearCaches(any(), any());
    }

    @Test
    void deleteForCurrentUserShouldAuthorizeAndEvictCaches() {
        Note note = new Note();
        note.setId(7L);
        when(noteRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(note));
        when(noteRepository.softDeleteById(7L)).thenReturn(1);

        noteCommandService.deleteForCurrentUser(7L);

        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void deleteForCurrentUserShouldThrowWhenNoteMissing() {
        when(noteRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.empty());

        assertThrows(
                NoteNotFoundException.class, () -> noteCommandService.deleteForCurrentUser(7L));

        verify(noteAuthorizationService, never()).ensureEditAccess(any(Note.class));
        verify(cacheProvider, never()).clearCaches(any(), any());
    }

    @Test
    void deleteForCurrentUserShouldThrowWhenSoftDeleteFails() {
        Note note = new Note();
        note.setId(7L);
        when(noteRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(note));
        when(noteRepository.softDeleteById(7L)).thenReturn(0);

        assertThrows(
                NoteNotFoundException.class, () -> noteCommandService.deleteForCurrentUser(7L));

        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(cacheProvider, never()).clearCaches(any(), any());
    }

    @Test
    void deleteShouldEvictCachesWhenSoftDeleteSucceeds() {
        when(noteRepository.softDeleteById(7L)).thenReturn(1);

        noteCommandService.delete(7L);

        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void changeOwnerShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> noteCommandService.changeOwner(1L, "bob"));

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void changeOwnerShouldUpdateOwnerAndReturnDto() {
        Note note = new Note();
        note.setId(1L);
        note.setOwner("alice");

        NoteDTO dto = sampleDto("t", "c", false, null, Set.of());

        when(userRepository.existsByUsername("bob")).thenReturn(true);
        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteCommandService.changeOwner(1L, "bob");

        assertThat(result).isEqualTo(dto);
        assertThat(note.getOwner()).isEqualTo("bob");
        verify(noteRepository).save(note);
    }

    @Test
    void changeOwnerShouldThrowWhenNoteMissing() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);
        when(noteRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteCommandService.changeOwner(1L, "bob"));

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void bulkShouldReturnEmptyWhenIdsEmptyAndNotEvictCaches() {
        BulkActionResult result =
                noteCommandService.bulk(
                        new BulkActionRequest(BulkAction.DELETE_SOFT.name(), Set.of()));

        assertThat(result.processedCount()).isZero();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(noteRepository);
        verifyNoMoreInteractions(cacheProvider);
    }

    @Test
    void bulkDeleteSoftShouldAddMissingIdsToFailedAndEvictCachesWhenProcessed() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        when(noteRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(active));
        when(noteRepository.softDeleteByIds(List.of(1L))).thenReturn(1);

        BulkActionResult result =
                noteCommandService.bulk(
                        new BulkActionRequest(BulkAction.DELETE_SOFT.name(), Set.of(1L, 2L)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactly(2L);
        verify(noteRepository).softDeleteByIds(List.of(1L));
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void bulkRestoreShouldRestoreOnlyDeletedAndNotEvictWhenNothingProcessed() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        when(noteRepository.findAllById(Set.of(1L))).thenReturn(List.of(active));

        BulkActionResult result =
                noteCommandService.bulk(
                        new BulkActionRequest(BulkAction.RESTORE.name(), Set.of(1L)));

        assertThat(result.processedCount()).isZero();
        assertThat(result.failedIds()).containsExactly(1L);
        verify(noteRepository, never()).restoreByIds(any());
        verify(cacheProvider, never()).clearCaches(any(), any());
    }

    @Test
    void bulkRestoreShouldRestoreDeletedAndEvictCaches() {
        Note deleted = new Note();
        deleted.setId(1L);
        deleted.setDeleted(true);

        when(noteRepository.findAllById(Set.of(1L))).thenReturn(List.of(deleted));
        when(noteRepository.restoreByIds(List.of(1L))).thenReturn(1);

        BulkActionResult result =
                noteCommandService.bulk(
                        new BulkActionRequest(BulkAction.RESTORE.name(), Set.of(1L)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedIds()).isEmpty();
        verify(noteRepository).restoreByIds(List.of(1L));
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void bulkDeleteForeverShouldDeleteOnlySoftDeleted() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        Note deleted = new Note();
        deleted.setId(2L);
        deleted.setDeleted(true);

        when(noteRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(active, deleted));

        BulkActionResult result =
                noteCommandService.bulk(
                        new BulkActionRequest(BulkAction.DELETE_FOREVER.name(), Set.of(1L, 2L)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactly(1L);
        verify(noteRepository).deleteAllByIdInBatch(List.of(2L));
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void bulkDeleteForeverShouldNotEvictWhenNothingDeleted() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        when(noteRepository.findAllById(Set.of(1L))).thenReturn(List.of(active));

        BulkActionResult result =
                noteCommandService.bulk(
                        new BulkActionRequest(BulkAction.DELETE_FOREVER.name(), Set.of(1L)));

        assertThat(result.processedCount()).isZero();
        assertThat(result.failedIds()).containsExactly(1L);
        verify(noteRepository, never()).deleteAllByIdInBatch(any());
        verify(cacheProvider, never()).clearCaches(any(), any());
    }

    @Test
    void bulkForCurrentUserShouldReturnEmptyWhenIdsEmptyAndNotEvictCaches() {
        BulkActionResult result =
                noteCommandService.bulkForCurrentUser(
                        new BulkActionRequest(BulkAction.DELETE_SOFT.name(), Set.of()));

        assertThat(result.processedCount()).isZero();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(noteRepository);
        verifyNoMoreInteractions(cacheProvider);
    }

    @Test
    void bulkForCurrentUserDeleteSoftShouldProcessOnlyNonDeletedAndReturnFailedIds() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        Note alreadyDeleted = new Note();
        alreadyDeleted.setId(2L);
        alreadyDeleted.setDeleted(true);

        when(noteRepository.findAllByIdInForCurrentUser(Set.of(1L, 2L, 3L)))
                .thenReturn(List.of(active, alreadyDeleted));
        when(noteRepository.softDeleteByIds(List.of(1L))).thenReturn(1);

        BulkActionResult result =
                noteCommandService.bulkForCurrentUser(
                        new BulkActionRequest(BulkAction.DELETE_SOFT.name(), Set.of(1L, 2L, 3L)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactlyInAnyOrder(2L, 3L);
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void bulkForCurrentUserRestoreShouldRestoreDeletedAndReturnFailedForActive() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        Note deleted = new Note();
        deleted.setId(2L);
        deleted.setDeleted(true);

        when(noteRepository.findAllByIdInForCurrentUser(Set.of(1L, 2L)))
                .thenReturn(List.of(active, deleted));
        when(noteRepository.restoreByIds(List.of(2L))).thenReturn(1);

        BulkActionResult result =
                noteCommandService.bulkForCurrentUser(
                        new BulkActionRequest(BulkAction.RESTORE.name(), Set.of(1L, 2L)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactly(1L);
        verify(noteRepository).restoreByIds(List.of(2L));
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    @Test
    void bulkForCurrentUserDeleteForeverShouldDeleteOnlySoftDeletedAndReturnFailedForActive() {
        Note active = new Note();
        active.setId(1L);
        active.setDeleted(false);

        Note deleted = new Note();
        deleted.setId(2L);
        deleted.setDeleted(true);

        when(noteRepository.findAllByIdInForCurrentUser(Set.of(1L, 2L)))
                .thenReturn(List.of(active, deleted));

        BulkActionResult result =
                noteCommandService.bulkForCurrentUser(
                        new BulkActionRequest(BulkAction.DELETE_FOREVER.name(), Set.of(1L, 2L)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedIds()).containsExactly(1L);
        verify(noteRepository).deleteAllByIdInBatch(List.of(2L));
        verify(cacheProvider).clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    private NoteDTO sampleDto(
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
}
