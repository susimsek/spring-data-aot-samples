package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.Optional;

public interface NoteRepository extends
        JpaRepository<Note, Long>,
        JpaSpecificationExecutor<Note>,
        SoftDeleteRepository<Note, Long>,
        RevisionRepository<Note, Long, Long> {

    @Override
    @EntityGraph(attributePaths = "tags")
    Page<Note> findAll(Specification<Note> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "tags")
    Optional<Note> findOne(Specification<Note> spec);
}
