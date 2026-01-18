package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface NoteShareTokenRepository
        extends JpaRepository<NoteShareToken, Long>, JpaSpecificationExecutor<NoteShareToken> {

    String NOTE_SHARE_TOKEN_BY_HASH_CACHE = "noteShareTokenByHash";

    @EntityGraph(attributePaths = {"note", "note.tags"})
    @Cacheable(
            cacheNames = NOTE_SHARE_TOKEN_BY_HASH_CACHE,
            key = "#tokenHash",
            unless = "#result == null")
    Optional<NoteShareToken> findOneWithNoteByTokenHashAndRevokedFalse(String tokenHash);

    @EntityGraph(attributePaths = {"note"})
    Optional<NoteShareToken> findOneWithNoteById(Long id);

    @Override
    @EntityGraph(attributePaths = {"note"})
    Page<NoteShareToken> findAll(@Nullable Specification<NoteShareToken> spec, Pageable pageable);

    @Query(
            """
            select t
            from NoteShareToken t
            where t.expiresAt < :now or t.revoked = true
            """)
    List<NoteShareToken> findExpiredOrRevoked(Instant now);
}
