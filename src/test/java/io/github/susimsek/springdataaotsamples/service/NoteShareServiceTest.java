package io.github.susimsek.springdataaotsamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import io.github.susimsek.springdataaotsamples.security.RandomUtils;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@ExtendWith(MockitoExtension.class)
class NoteShareServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private NoteShareTokenRepository noteShareTokenRepository;
    @Mock private NoteAuthorizationService noteAuthorizationService;
    @Mock private CacheProvider cacheProvider;

    @InjectMocks private NoteShareService noteShareService;

    @Test
    void createShouldPersistTokenAndReturnDto() {
        Note note = new Note();
        note.setId(10L);
        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        NoteShareToken saved = new NoteShareToken();
        saved.setId(5L);
        saved.setNote(note);
        saved.setPermission(SharePermission.READ);
        saved.setExpiresAt(null);
        saved.setOneTime(true);
        saved.setUseCount(0);
        saved.setRevoked(false);
        when(noteShareTokenRepository.save(any())).thenReturn(saved);
        CreateShareTokenRequest request = new CreateShareTokenRequest(null, true, true);

        try (MockedStatic<RandomUtils> random = mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.hexToken(32)).thenReturn("raw-token");

            NoteShareDTO dto = noteShareService.create(10L, request);

            assertThat(dto.token()).isEqualTo("raw-token");
            assertThat(dto.oneTime()).isTrue();
            assertThat(dto.noteId()).isEqualTo(10L);
            verify(cacheProvider)
                    .clearCache(
                            eq(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE),
                            eq("raw-token"));
        }
    }

    @Test
    void createForCurrentUserShouldCheckAuthorization() {
        Note note = new Note();
        note.setId(1L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        NoteShareToken saved = new NoteShareToken();
        saved.setNote(note);
        when(noteShareTokenRepository.save(any())).thenReturn(saved);
        CreateShareTokenRequest request = new CreateShareTokenRequest(Instant.now(), false, false);

        try (MockedStatic<RandomUtils> random = mockStatic(RandomUtils.class)) {
            random.when(() -> RandomUtils.hexToken(32)).thenReturn("raw");
            noteShareService.createForCurrentUser(1L, request);
            verify(noteAuthorizationService).ensureEditAccess(note);
        }
    }

    @Test
    void listAllForCurrentUserShouldUseCurrentLogin() {
        Note note = new Note();
        note.setId(3L);
        note.setTitle("t");
        note.setOwner("alice");
        NoteShareToken token = new NoteShareToken();
        token.setId(11L);
        token.setNote(note);
        token.setTokenHash("abc");
        final Pageable pageable = PageRequest.of(0, 5);
        Page<NoteShareToken> page = new PageImpl<>(List.of(token), pageable, 1);
        when(noteShareTokenRepository.findAllWithNote(
                        ArgumentMatchers.<Specification<NoteShareToken>>any(), any(Pageable.class)))
                .thenReturn(page);
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));

            Page<NoteShareDTO> result =
                    noteShareService.listAllForCurrentUser(pageable, null, null, null, null);

            assertThat(result.getContent())
                    .singleElement()
                    .satisfies(
                            dto -> {
                                assertThat(dto.noteId()).isEqualTo(3L);
                                assertThat(dto.noteTitle()).isEqualTo("t");
                            });
        }
    }

    @Test
    void revokeShouldMarkTokenAndClearCache() {
        Note note = new Note();
        NoteShareToken token = new NoteShareToken();
        token.setId(2L);
        token.setNote(note);
        token.setTokenHash("hash");
        token.setRevoked(false);
        when(noteShareTokenRepository.findOneWithNoteById(2L)).thenReturn(Optional.of(token));

        noteShareService.revoke(2L);

        assertThat(token.isRevoked()).isTrue();
        verify(noteShareTokenRepository).save(token);
        verify(cacheProvider)
                .clearCache(
                        eq(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE), eq("hash"));
    }

    @Test
    void revokeForCurrentUserShouldCheckAccess() {
        Note note = new Note();
        NoteShareToken token = new NoteShareToken();
        token.setId(2L);
        token.setNote(note);
        token.setRevoked(false);
        when(noteShareTokenRepository.findOneWithNoteById(2L)).thenReturn(Optional.of(token));

        noteShareService.revokeForCurrentUser(2L);

        verify(noteAuthorizationService).ensureEditAccess(note);
    }

    @Test
    void validateAndConsumeShouldCheckExpiryAndRevocation() {
        Note note = new Note();
        note.setDeleted(false);
        NoteShareToken token = new NoteShareToken();
        token.setId(3L);
        token.setNote(note);
        token.setTokenHash("hash");
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token.setUseCount(0);
        token.setOneTime(true);
        when(noteShareTokenRepository.findOneWithNoteByTokenHashAndRevokedFalse("raw"))
                .thenReturn(Optional.of(token));
        when(noteShareTokenRepository.saveAndFlush(token)).thenReturn(token);

        NoteShareToken result = noteShareService.validateAndConsume("raw");

        assertThat(result.getUseCount()).isEqualTo(1);
        assertThat(result.isRevoked()).isTrue();
        verify(cacheProvider)
                .clearCache(eq(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE), eq("raw"));
    }

    @Test
    void validateAndConsumeShouldTryHashFallback() {
        Note note = new Note();
        NoteShareToken token = new NoteShareToken();
        token.setId(4L);
        token.setNote(note);
        token.setUseCount(0);
        when(noteShareTokenRepository.findOneWithNoteByTokenHashAndRevokedFalse("raw"))
                .thenReturn(Optional.of(token));

        noteShareService.validateAndConsume("raw");

        verify(noteShareTokenRepository).saveAndFlush(token);
        verify(cacheProvider)
                .clearCache(eq(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE), eq("raw"));
    }

    @Test
    void validateAndConsumeShouldRejectExpiredOrDeletedNote() {
        Note note = new Note();
        note.setDeleted(true);
        NoteShareToken token = new NoteShareToken();
        token.setNote(note);
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(noteShareTokenRepository.findOneWithNoteByTokenHashAndRevokedFalse("raw"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> noteShareService.validateAndConsume("raw"))
                .isInstanceOf(InvalidBearerTokenException.class);
        verifyNoInteractions(cacheProvider);
    }

    @Test
    void listForCurrentUserShouldCheckAccess() {
        Note note = new Note();
        note.setId(1L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteShareTokenRepository.findAllWithNote(
                        ArgumentMatchers.<Specification<NoteShareToken>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));

        noteShareService.listForCurrentUser(1L, PageRequest.of(0, 5), null, null, null, null);

        verify(noteAuthorizationService).ensureEditAccess(note);
    }

    @Test
    void listForAdminShouldThrowWhenNoteMissing() {
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 5);
        ThrowingCallable call =
                () -> noteShareService.listForAdmin(1L, pageable, null, null, null, null);
        assertThatThrownBy(call).isInstanceOf(NoteNotFoundException.class);
    }
}
