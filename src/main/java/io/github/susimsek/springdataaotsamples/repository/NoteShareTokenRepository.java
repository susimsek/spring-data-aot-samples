package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.custom.NoteShareTokenRepositoryCustom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface NoteShareTokenRepository
        extends JpaRepository<NoteShareToken, Long>,
                JpaSpecificationExecutor<NoteShareToken>,
                NoteShareTokenRepositoryCustom {

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

    @EntityGraph(attributePaths = {"note"})
    List<NoteShareToken> findAllByIdIn(List<Long> ids);

    default Page<NoteShareToken> findAllWithNote(
            Specification<NoteShareToken> spec, Pageable pageable) {
        Page<Long> idPage = findIds(spec, pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        var ids = idPage.getContent();
        var loadedTokens = findAllByIdIn(ids);
        var tokenMap =
                loadedTokens.stream()
                        .collect(Collectors.toMap(NoteShareToken::getId, t -> t, (a, _) -> a));
        var ordered = ids.stream().map(tokenMap::get).toList();
        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Query(
            """
            select t
            from NoteShareToken t
            where t.expiresAt < :now or t.revoked = true
            """)
    List<NoteShareToken> findExpiredOrRevoked(Instant now);
}
