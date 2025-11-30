package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.custom.SoftDeleteRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.history.RevisionRepository;

public interface NoteRepository extends
        JpaRepository<Note, Long>,
        JpaSpecificationExecutor<Note>,
        SoftDeleteRepository<Note, Long>,
        RevisionRepository<Note, Long, Long> {
}
