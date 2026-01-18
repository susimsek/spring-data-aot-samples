package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface NoteRepositoryCustom {

    Page<Note> findAllWithTags(Specification<Note> specification, Pageable pageable);
}
