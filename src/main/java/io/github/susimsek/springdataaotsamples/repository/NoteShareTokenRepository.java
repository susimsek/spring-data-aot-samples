package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface NoteShareTokenRepository extends JpaRepository<NoteShareToken, Long>, JpaSpecificationExecutor<NoteShareToken> {

    String NOTE_SHARE_TOKEN_BY_HASH_CACHE = "noteShareTokenByHash";

    @EntityGraph(attributePaths = {"note", "note.tags"})
    @Cacheable(cacheNames = NOTE_SHARE_TOKEN_BY_HASH_CACHE, key = "#tokenHash", unless = "#result == null")
    Optional<NoteShareToken> findOneWithNoteByTokenHashAndRevokedFalse(String tokenHash);

    @EntityGraph(attributePaths = {"note"})
    Optional<NoteShareToken> findOneWithNoteById(Long id);

    long deleteByExpiresAtBefore(Instant now);

    long deleteByRevokedTrue();
}
