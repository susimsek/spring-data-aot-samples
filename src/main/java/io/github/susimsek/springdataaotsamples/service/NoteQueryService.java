package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications.isDeleted;
import static io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications.isNotDeleted;
import static io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications.prioritizePinned;
import static io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications.search;

@Service
@RequiredArgsConstructor
public class NoteQueryService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    @Transactional(readOnly = true)
    public Page<NoteDTO> find(NoteCriteria criteria, Pageable pageable) {
        var specification = createSpecification(criteria);
        var pageableWithPinned = prioritizePinned(pageable);
        return noteRepository.findAll(specification, pageableWithPinned).map(noteMapper::toDto);
    }

    private Specification<Note> createSpecification(NoteCriteria criteria) {
        boolean deleted = Boolean.TRUE.equals(criteria.deleted());
        String query = criteria.query();

        Specification<Note> spec = deleted
                ? Specification.where(isDeleted())
                : Specification.where(isNotDeleted());

        return spec.and(search(query));
    }
}
