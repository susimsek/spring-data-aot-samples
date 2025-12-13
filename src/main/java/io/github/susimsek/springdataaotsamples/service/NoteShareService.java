package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import io.github.susimsek.springdataaotsamples.security.HashingUtils;
import io.github.susimsek.springdataaotsamples.security.RandomUtils;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NoteShareService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final NoteRepository noteRepository;
    private final NoteShareTokenRepository noteShareTokenRepository;
    private final NoteAuthorizationService noteAuthorizationService;

    @Transactional
    public NoteShareDTO createForCurrentUser(Long noteId, CreateShareTokenRequest request) {
        var note = loadNote(noteId);
        noteAuthorizationService.ensureEditAccess(note);
        return create(note, request);
    }

    @Transactional
    public NoteShareDTO create(Long noteId, CreateShareTokenRequest request) {
        var note = loadNote(noteId);
        return create(note, request);
    }

    @Transactional
    public void revokeForCurrentUser(Long tokenId) {
        var token = noteShareTokenRepository.findOneWithNoteById(tokenId)
            .orElseThrow(() -> new InvalidBearerTokenException("Share token not found"));
        noteAuthorizationService.ensureEditAccess(token.getNote());
        if (token.isRevoked()) {
            return;
        }
        token.setRevoked(true);
        noteShareTokenRepository.save(token);
    }

    @Transactional
    public void revoke(Long tokenId) {
        var token = noteShareTokenRepository.findOneWithNoteById(tokenId)
            .orElseThrow(() -> new InvalidBearerTokenException("Share token not found"));
        if (token.isRevoked()) {
            return;
        }
        token.setRevoked(true);
        noteShareTokenRepository.save(token);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NoteShareToken validateAndConsume(String rawToken) {
        Assert.hasText(rawToken, "Share token must not be empty");
        String tokenHash = HashingUtils.sha256Hex(rawToken);
        var token = noteShareTokenRepository.findOneWithNoteByTokenHashAndRevokedFalse(tokenHash)
            .orElseThrow(() -> new InvalidBearerTokenException("Invalid share token"));

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidBearerTokenException("Share token expired");
        }
        Note note = token.getNote();
        if (note.isDeleted()) {
            throw new InvalidBearerTokenException("Note is not accessible");
        }

        token.setUseCount(token.getUseCount() + 1);
        if (token.isOneTime()) {
            token.setRevoked(true);
        }
        noteShareTokenRepository.saveAndFlush(token);
        return token;
    }

    private NoteShareDTO create(Note note, @NotNull CreateShareTokenRequest request) {
        if (note.isDeleted()) {
            throw new InvalidBearerTokenException("Cannot share a deleted note");
        }
        Instant now = Instant.now();
        Instant expiresAt;
        if (Boolean.TRUE.equals(request.noExpiry())) {
            expiresAt = null;
        } else if (request.expiresAt() != null) {
            expiresAt = request.expiresAt();
        } else {
            expiresAt = now.plus(DEFAULT_TTL);
        }

        String rawToken = RandomUtils.hexToken(32);
        NoteShareToken shareToken = new NoteShareToken();
        shareToken.setNote(note);
        shareToken.setPermission(SharePermission.READ);
        shareToken.setTokenHash(HashingUtils.sha256Hex(rawToken));
        shareToken.setExpiresAt(expiresAt);
        shareToken.setOneTime(Boolean.TRUE.equals(request.oneTime()));
        shareToken.setUseCount(0);
        shareToken.setRevoked(false);

        NoteShareToken saved = noteShareTokenRepository.save(shareToken);

        return new NoteShareDTO(
                saved.getId(),
                rawToken,
                note.getId(),
                saved.getPermission(),
                saved.getExpiresAt(),
                saved.isOneTime()
        );
    }

    private Note loadNote(Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
    }
}
