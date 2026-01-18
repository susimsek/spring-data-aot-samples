package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import io.github.susimsek.springdataaotsamples.security.HashingUtils;
import io.github.susimsek.springdataaotsamples.security.RandomUtils;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.dto.CreateShareTokenRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteShareDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.spec.NoteShareTokenSpecifications;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoteShareService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final NoteRepository noteRepository;
    private final NoteShareTokenRepository noteShareTokenRepository;
    private final NoteAuthorizationService noteAuthorizationService;
    private final CacheProvider cacheProvider;

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
        shareToken.setTokenHash(rawToken);
        shareToken.setExpiresAt(expiresAt);
        shareToken.setOneTime(Boolean.TRUE.equals(request.oneTime()));
        shareToken.setUseCount(0);
        shareToken.setRevoked(false);

        NoteShareToken saved = noteShareTokenRepository.save(shareToken);
        evictShareTokenCache(rawToken);

        return toDto(saved, rawToken);
    }

    @Transactional(readOnly = true)
    public Page<NoteShareDTO> listForCurrentUser(
            Long noteId,
            Pageable pageable,
            String q,
            String status,
            Instant createdFrom,
            Instant createdTo) {
        var note = loadNote(noteId);
        noteAuthorizationService.ensureEditAccess(note);
        return fetchPage(noteId, pageable, q, status, createdFrom, createdTo);
    }

    @Transactional(readOnly = true)
    public Page<NoteShareDTO> listForAdmin(
            Long noteId,
            Pageable pageable,
            String q,
            String status,
            Instant createdFrom,
            Instant createdTo) {
        loadNote(noteId);
        return fetchPage(noteId, pageable, q, status, createdFrom, createdTo);
    }

    @Transactional(readOnly = true)
    public Page<NoteShareDTO> listAllForAdmin(
            Pageable pageable, String q, String status, Instant createdFrom, Instant createdTo) {
        Pageable effective = resolvePageable(pageable);
        var page = searchAll(q, status, createdFrom, createdTo, effective);
        List<NoteShareDTO> content = page.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(content, effective, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<NoteShareDTO> listAllForCurrentUser(
            Pageable pageable, String q, String status, Instant createdFrom, Instant createdTo) {
        Pageable effective = resolvePageable(pageable);
        String login =
                SecurityUtils.getCurrentUserLogin()
                        .orElseThrow(
                                () -> new InvalidBearerTokenException("Current user not found"));
        var page = searchForOwner(login, q, status, createdFrom, createdTo, effective);
        List<NoteShareDTO> content = page.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(content, effective, page.getTotalElements());
    }

    @Transactional
    public void revokeForCurrentUser(Long tokenId) {
        var token =
                noteShareTokenRepository
                        .findOneWithNoteById(tokenId)
                        .orElseThrow(
                                () -> new InvalidBearerTokenException("Share token not found"));
        noteAuthorizationService.ensureEditAccess(token.getNote());
        if (token.isRevoked()) {
            return;
        }
        token.setRevoked(true);
        noteShareTokenRepository.save(token);
        evictShareTokenCache(token.getTokenHash());
    }

    @Transactional
    public void revoke(Long tokenId) {
        var token =
                noteShareTokenRepository
                        .findOneWithNoteById(tokenId)
                        .orElseThrow(
                                () -> new InvalidBearerTokenException("Share token not found"));
        if (token.isRevoked()) {
            return;
        }
        token.setRevoked(true);
        noteShareTokenRepository.save(token);
        evictShareTokenCache(token.getTokenHash());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NoteShareToken validateAndConsume(String rawToken) {
        var token =
                noteShareTokenRepository
                        .findOneWithNoteByTokenHashAndRevokedFalse(rawToken)
                        .or(
                                () ->
                                        noteShareTokenRepository
                                                .findOneWithNoteByTokenHashAndRevokedFalse(
                                                        HashingUtils.sha256Hex(rawToken)))
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
        evictShareTokenCache(rawToken, HashingUtils.sha256Hex(rawToken), token.getTokenHash());
        return token;
    }

    private void evictShareTokenCache(String... keys) {
        for (String key : keys) {
            if (StringUtils.hasText(key)) {
                cacheProvider.clearCache(
                        NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE, key);
            }
        }
    }

    private Note loadNote(Long noteId) {
        return noteRepository.findById(noteId).orElseThrow(() -> new NoteNotFoundException(noteId));
    }

    private NoteShareDTO toDto(NoteShareToken token) {
        return toDto(token, token.getTokenHash());
    }

    private NoteShareDTO toDto(NoteShareToken token, String tokenValue) {
        boolean expired =
                token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now());
        return new NoteShareDTO(
                token.getId(),
                tokenValue,
                token.getNote().getId(),
                token.getPermission(),
                token.getExpiresAt(),
                token.isOneTime(),
                token.isRevoked(),
                token.getUseCount(),
                token.getCreatedDate(),
                token.getLastModifiedDate(),
                expired,
                token.getNote().getTitle(),
                token.getNote().getOwner());
    }

    private Page<NoteShareDTO> fetchPage(
            Long noteId,
            Pageable pageable,
            String q,
            String status,
            Instant createdFrom,
            Instant createdTo) {
        Pageable effective = resolvePageable(pageable);
        Specification<NoteShareToken> spec =
                Specification.where(NoteShareTokenSpecifications.forNote(noteId))
                        .and(NoteShareTokenSpecifications.search(q))
                        .and(NoteShareTokenSpecifications.status(status))
                        .and(NoteShareTokenSpecifications.createdBetween(createdFrom, createdTo));
        var page = noteShareTokenRepository.findAll(spec, effective);
        List<NoteShareDTO> content = page.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(content, effective, page.getTotalElements());
    }

    private Page<NoteShareToken> searchForOwner(
            String owner,
            String q,
            String status,
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable) {
        Specification<NoteShareToken> spec =
                Specification.where(NoteShareTokenSpecifications.ownedBy(owner))
                        .and(NoteShareTokenSpecifications.search(q))
                        .and(NoteShareTokenSpecifications.status(status))
                        .and(NoteShareTokenSpecifications.createdBetween(createdFrom, createdTo));
        return noteShareTokenRepository.findAll(spec, pageable);
    }

    private Page<NoteShareToken> searchAll(
            String q, String status, Instant createdFrom, Instant createdTo, Pageable pageable) {
        Specification<NoteShareToken> spec =
                Specification.where(NoteShareTokenSpecifications.search(q))
                        .and(NoteShareTokenSpecifications.status(status))
                        .and(NoteShareTokenSpecifications.createdBetween(createdFrom, createdTo));
        return noteShareTokenRepository.findAll(spec, pageable);
    }

    private Pageable resolvePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdDate"));
        }
        return pageable;
    }
}
