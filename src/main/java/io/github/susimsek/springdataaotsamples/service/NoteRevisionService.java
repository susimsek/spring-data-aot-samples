package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.RevisionNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteRevisionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.RevisionSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteRevisionService {

    private final NoteRepository noteRepository;
    private final NoteRevisionMapper noteRevisionMapper;
    private final NoteAuthorizationService noteAuthorizationService;

    @Transactional(readOnly = true)
    public Page<NoteRevisionDTO> findRevisions(Long id, Pageable pageable) {
        var note = findNote(id);
        return findRevisionsInternal(note.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public NoteRevisionDTO findRevision(Long id, Long revisionNumber) {
        var revision = noteRepository.findRevision(id, revisionNumber)
                .orElseThrow(() -> new RevisionNotFoundException(id, revisionNumber));
        return noteRevisionMapper.toRevisionDto(revision);
    }

    @Transactional(readOnly = true)
    public Page<NoteRevisionDTO> findRevisionsForCurrentUser(Long id, Pageable pageable) {
        var note = findNote(id);
        noteAuthorizationService.ensureOwner(note);
        return findRevisionsInternal(note.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public NoteRevisionDTO findRevisionForCurrentUser(Long id, Long revisionNumber) {
        var note = findNote(id);
        noteAuthorizationService.ensureOwner(note);
        var revision = noteRepository.findRevision(id, revisionNumber)
                .orElseThrow(() -> new RevisionNotFoundException(id, revisionNumber));
        return noteRevisionMapper.toRevisionDto(revision);
    }

    private Note findNote(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    private Page<NoteRevisionDTO> findRevisionsInternal(Long noteId, Pageable pageable) {
        var pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                RevisionSort.desc());
        var revisions = noteRepository.findRevisions(noteId, pageRequest);
        return noteRevisionMapper.toRevisionDtoPage(revisions);
    }

}
