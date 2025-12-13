package io.github.susimsek.springdataaotsamples.service.query;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.NoteAuthorizationService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoteQueryService {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final NoteAuthorizationService noteAuthorizationService;

    @Transactional(readOnly = true)
    public Page<NoteDTO> findAll(Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return find(new NoteCriteria(query, false, tags, color, pinned, null), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findAllForCurrentUser(Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return find(new NoteCriteria(query, false, tags, color, pinned, getCurrentUsername()), pageable);
    }

    @Transactional(readOnly = true)
    public NoteDTO findById(Long id) {
        var note = findActiveNote(id);
        return noteMapper.toDto(note);
    }

    @Transactional(readOnly = true)
    public NoteDTO findByIdForCurrentUser(Long id) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureReadAccess(note);
        return noteMapper.toDto(note);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findByCriteria(NoteCriteria criteria, Pageable pageable) {
        return find(criteria, pageable);
    }

    private Specification<Note> createSpecification(NoteCriteria criteria) {
        boolean deleted = Boolean.TRUE.equals(criteria.deleted());
        String query = criteria.query();
        var tags = criteria.tags();
        var color = criteria.color();
        var pinned = criteria.pinned();
        var owner = criteria.owner();

        Specification<Note> spec = deleted
                ? Specification.where(NoteSpecifications.isDeleted())
                : Specification.where(NoteSpecifications.isNotDeleted());

        return spec
            .and(NoteSpecifications.search(query))
            .and(NoteSpecifications.hasColor(color))
            .and(NoteSpecifications.isPinned(pinned))
            .and(NoteSpecifications.ownedBy(owner))
            .and(NoteSpecifications.hasTags(tags));
    }

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }

    private Note findActiveNote(Long id) {
        return noteRepository.findOne(
                        Specification.where(NoteSpecifications.isNotDeleted())
                                .and((root, cq, cb) -> cb.equal(root.get("id"), id)))
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    private Page<NoteDTO> find(NoteCriteria criteria, Pageable pageable) {
        var specification = createSpecification(criteria);
        var pageableWithPinned = NoteSpecifications.prioritizePinned(pageable);
        return noteRepository.findAllWithTags(specification, pageableWithPinned)
            .map(noteMapper::toDto);
    }
}
