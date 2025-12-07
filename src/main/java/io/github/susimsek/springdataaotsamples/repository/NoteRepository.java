package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.custom.NoteRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface NoteRepository extends
        JpaRepository<Note, Long>,
        JpaSpecificationExecutor<Note>,
       NoteRepositoryCustom,
        SoftDeleteRepository<Note, Long>,
        RevisionRepository<Note, Long, Long> {

    @Override
    @EntityGraph(attributePaths = "tags")
    Optional<Note> findOne(Specification<Note> spec);

    @EntityGraph(attributePaths = "tags")
    List<Note> findAllByIdIn(List<Long> ids);

    @Query("select n from Note n where n.id in :ids and n.owner = ?#{authentication.name}")
    List<Note> findAllByIdInForCurrentUser(@Param("ids") Iterable<Long> ids);

    default Page<Note> findAllWithTags(Specification<Note> spec,
                                       Pageable pageable) {

        Page<Long> idPage = findIds(spec, pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        var ids = idPage.getContent();

        var loadedNotes = findAllByIdIn(ids);

        var noteMap = loadedNotes.stream()
            .collect(Collectors.toMap(
                Note::getId,
                n -> n,
                (a, b) -> a
            ));


        var ordered = ids.stream()
            .map(noteMap::get)
            .toList();

        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Modifying
    @Transactional
    @Query("delete from Note n where n.deleted = true and n.owner = :owner")
    void purgeDeletedByOwner(@Param("owner") String owner);
}
