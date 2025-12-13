package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteShareTokenRepository extends JpaRepository<NoteShareToken, Long> {

    @EntityGraph(attributePaths = {"note", "note.tags"})
    Optional<NoteShareToken> findOneWithNoteByTokenHashAndRevokedFalse(String tokenHash);

    @EntityGraph(attributePaths = {"note"})
    Optional<NoteShareToken> findOneWithNoteById(Long id);

    @EntityGraph(attributePaths = {"note"})
    Page<NoteShareToken> findAllByNoteId(Long noteId, Pageable pageable);

    @EntityGraph(attributePaths = {"note"})
    Page<NoteShareToken> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"note"})
    Page<NoteShareToken> findAllByNoteOwner(String owner, Pageable pageable);
}
