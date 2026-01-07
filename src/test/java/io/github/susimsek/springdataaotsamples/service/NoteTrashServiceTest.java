package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.command.TagCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.query.NoteQueryService;
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

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

            assertThatThrownBy(
                            () ->
                                    noteTrashService.findDeletedForCurrentUser(
                                            pageable, "q", Set.of(), null, null))
                    .isInstanceOf(UsernameNotFoundException.class);
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

        verify(cacheProvider)
                .clearCaches(
                        Note.class.getName(), Note.class.getName() + ".tags", Tag.class.getName());
    }

    @Test
    void restoreForCurrentUserShouldCheckAuthorizationAndDeletedFlag() {
        Note note = new Note();
        note.setDeleted(true);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.restoreById(1L)).thenReturn(1);

        noteTrashService.restoreForCurrentUser(1L);

        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(cacheProvider)
                .clearCaches(
                        Note.class.getName(), Note.class.getName() + ".tags", Tag.class.getName());
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
        noteTrashService.emptyTrash();

        verify(noteRepository).purgeDeleted();
        verify(tagCommandService).cleanupOrphanTagsAsync();
        verify(cacheProvider)
                .clearCaches(
                        Note.class.getName(), Note.class.getName() + ".tags", Tag.class.getName());
    }

    @Test
    void emptyTrashForCurrentUserShouldUseCurrentUser() {
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));

            noteTrashService.emptyTrashForCurrentUser();

            verify(noteRepository).purgeDeletedByOwner("alice");
            verify(tagCommandService).cleanupOrphanTagsAsync();
        }
    }

    @Test
    void deletePermanentlyShouldValidateDeletedFlag() {
        Note note = new Note();
        note.setDeleted(false);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteTrashService.deletePermanently(1L))
                .isInstanceOf(InvalidPermanentDeleteException.class);
    }

    @Test
    void deletePermanentlyShouldDeleteWhenDeleted() {
        Note note = new Note();
        note.setDeleted(true);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        noteTrashService.deletePermanently(1L);

        verify(noteRepository).deleteById(1L);
        verify(tagCommandService).cleanupOrphanTagsAsync();
        verify(cacheProvider)
                .clearCaches(
                        Note.class.getName(), Note.class.getName() + ".tags", Tag.class.getName());
    }

    @Test
    void deletePermanentlyForCurrentUserShouldCheckOwnerAndDeletedFlag() {
        Note note = new Note();
        note.setDeleted(true);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        noteTrashService.deletePermanentlyForCurrentUser(1L);

        verify(noteAuthorizationService).ensureEditAccess(note);
        verify(noteRepository).deleteById(1L);
    }
}
