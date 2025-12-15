package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface NoteShareTokenRepository extends JpaRepository<NoteShareToken, Long>, JpaSpecificationExecutor<NoteShareToken> {

    @EntityGraph(attributePaths = {"note", "note.tags"})
    Optional<NoteShareToken> findOneWithNoteByTokenHashAndRevokedFalse(String tokenHash);

    @EntityGraph(attributePaths = {"note"})
    Optional<NoteShareToken> findOneWithNoteById(Long id);

    long deleteByExpiresAtBefore(Instant now);

    long deleteByRevokedTrue();
}
