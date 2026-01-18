package io.github.susimsek.springdataaotsamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.command.TagCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.query.NoteQueryService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class NoteTrashServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private TagCommandService tagCommandService;
    @Mock private NoteAuthorizationService noteAuthorizationService;
    @Mock private NoteQueryService noteQueryService;
    @Mock private CacheProvider cacheProvider;

    @InjectMocks private NoteTrashService noteTrashService;

    @AfterEach
    void tearDown() {
        // clear static mocks if used
    }

    @Test
    void findDeletedShouldDelegateToQueryServiceWithDeletedCriteria() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<NoteDTO> page = new PageImpl<>(Set.<NoteDTO>of().stream().toList());
        when(noteQueryService.findByCriteria(any(NoteCriteria.class), eq(pageable)))
                .thenReturn(page);

        noteTrashService.findDeleted(pageable, "q", Set.of("t"), "#123", false);

        ArgumentCaptor<NoteCriteria> critCaptor = ArgumentCaptor.forClass(NoteCriteria.class);
        verify(noteQueryService).findByCriteria(critCaptor.capture(), eq(pageable));
        assertThat(critCaptor.getValue().deleted()).isTrue();
        assertThat(critCaptor.getValue().owner()).isNull();
    }

    @Test
    void findDeletedForCurrentUserShouldSetOwnerFromSecurityContext() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<NoteDTO> page = new PageImpl<>(Set.<NoteDTO>of().stream().toList());
        when(noteQueryService.findByCriteria(any(NoteCriteria.class), eq(pageable)))
                .thenReturn(page);
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));

            noteTrashService.findDeletedForCurrentUser(pageable, "q", Set.of("t"), "#123", false);

            ArgumentCaptor<NoteCriteria> critCaptor = ArgumentCaptor.forClass(NoteCriteria.class);
            verify(noteQueryService).findByCriteria(critCaptor.capture(), eq(pageable));
            assertThat(critCaptor.getValue().owner()).isEqualTo("alice");
        }
    }

    @Test
    void findDeletedForCurrentUserShouldThrowWhenUserMissing() {
        Pageable pageable = PageRequest.of(0, 5);
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            ThrowingCallable call =
                    () ->
                            noteTrashService.findDeletedForCurrentUser(
                                    pageable, "q", Set.of(), null, null);
            assertThatThrownBy(call).isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Test
    void restoreShouldThrowWhenNotFound() {
        when(noteRepository.restoreById(1L)).thenReturn(0);
        assertThatThrownBy(() -> noteTrashService.restore(1L))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void restoreShouldEvictCachesOnSuccess() {
        when(noteRepository.restoreById(1L)).thenReturn(1);

        noteTrashService.restore(1L);

        verify(cacheProvider).clearCache(Note.class.getName(), 1L);
        verify(cacheProvider).clearCache(NoteRepository.NOTE_BY_ID_CACHE, 1L);
    }

    @Test
    void restoreForCurrentUserShouldCheckAuthorizationAndDeletedFlag() {
        Note note = new Note();
        note.setDeleted(true);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.restoreById(1L)).thenReturn(1);

        noteTrashService.restoreForCurrentUser(1L);

        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(cacheProvider).clearCache(Note.class.getName(), 1L);
        verify(cacheProvider).clearCache(NoteRepository.NOTE_BY_ID_CACHE, 1L);
    }

    @Test
    void restoreForCurrentUserShouldFailWhenNotDeleted() {
        Note note = new Note();
        note.setDeleted(false);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteTrashService.restoreForCurrentUser(1L))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void emptyTrashShouldPurgeAndCleanup() {
        when(noteRepository.findDeletedIds()).thenReturn(List.of(1L, 2L));

        noteTrashService.emptyTrash();

        verify(noteRepository).findDeletedIds();
        verify(noteRepository).purgeDeleted();
        verify(tagCommandService).cleanupOrphanTagsAsync();
        verify(cacheProvider).clearCache(Note.class.getName(), List.of(1L, 2L));
        verify(cacheProvider).clearCache(NoteRepository.NOTE_BY_ID_CACHE, List.of(1L, 2L));
    }

    @Test
    void emptyTrashForCurrentUserShouldUseCurrentUser() {
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));
            when(noteRepository.findDeletedIdsByOwner("alice")).thenReturn(List.of(7L));

            noteTrashService.emptyTrashForCurrentUser();

            verify(noteRepository).findDeletedIdsByOwner("alice");
            verify(noteRepository).purgeDeletedByOwner("alice");
            verify(tagCommandService).cleanupOrphanTagsAsync();
            verify(cacheProvider).clearCache(Note.class.getName(), List.of(7L));
            verify(cacheProvider).clearCache(NoteRepository.NOTE_BY_ID_CACHE, List.of(7L));
        }
    }

    @Test
    void deletePermanentlyShouldValidateDeletedFlag() {
        when(noteRepository.deletePermanentlyById(1L)).thenReturn(0);
        when(noteRepository.findDeletedFlagById(1L)).thenReturn(Optional.of(false));

        assertThatThrownBy(() -> noteTrashService.deletePermanently(1L))
                .isInstanceOf(InvalidPermanentDeleteException.class);
    }

    @Test
    void deletePermanentlyShouldDeleteWhenDeleted() {
        when(noteRepository.deletePermanentlyById(1L)).thenReturn(1);

        noteTrashService.deletePermanently(1L);

        verify(noteRepository).deletePermanentlyById(1L);
        verify(tagCommandService).cleanupOrphanTagsAsync();
        verify(cacheProvider).clearCache(Note.class.getName(), 1L);
        verify(cacheProvider).clearCache(NoteRepository.NOTE_BY_ID_CACHE, 1L);
    }

    @Test
    void deletePermanentlyForCurrentUserShouldCheckOwnerAndDeletedFlag() {
        when(noteRepository.deletePermanentlyByIdForCurrentUser(1L)).thenReturn(1);

        noteTrashService.deletePermanentlyForCurrentUser(1L);

        verify(noteRepository).deletePermanentlyByIdForCurrentUser(1L);
    }
}
