package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.enumeration.BulkAction;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.RevisionNotFoundException;
import lombok.RequiredArgsConstructor;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteRevisionMapper;
import io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteQueryService noteQueryService;
    private final TagService tagService;
    private final NoteMapper noteMapper;
    private final NoteRevisionMapper noteRevisionMapper;

    @Transactional
    public NoteDTO create(NoteCreateRequest request) {
        var note = noteMapper.toEntity(request);
        note.setTags(tagService.resolveTags(request.tags()));
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO update(Long id, NoteUpdateRequest request) {
        var note = findActiveNote(id);
        noteMapper.updateEntity(request, note);
        note.setTags(tagService.resolveTags(request.tags()));
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO patch(Long id, NotePatchRequest request) {
        var note = findActiveNote(id);
        noteMapper.patchEntity(request, note);
        if (request.tags() != null) {
            note.setTags(tagService.resolveTags(request.tags()));
        }
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<NoteRevisionDTO> findRevisions(Long id, Pageable pageable) {
        if (!noteRepository.existsById(id)) {
            throw new NoteNotFoundException(id);
        }
        var pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                RevisionSort.desc());
        var revisions = noteRepository.findRevisions(id, pageRequest);
        return noteRevisionMapper.toRevisionDtoPage(revisions);
    }

    @Transactional(readOnly = true)
    public NoteRevisionDTO findRevision(Long id, Long revisionNumber) {
        var revision = noteRepository.findRevision(id, revisionNumber)
                .orElseThrow(() -> new RevisionNotFoundException(id, revisionNumber));
        return noteRevisionMapper.toRevisionDto(revision);
    }

    @Transactional
    public NoteDTO restoreRevision(Long id, Long revisionNumber) {
        var revision = noteRepository.findRevision(id, revisionNumber)
                .orElseThrow(() -> new RevisionNotFoundException(id, revisionNumber));
        var snapshot = revision.getEntity();
        var note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        noteMapper.applyRevision(snapshot, note);

        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findAll(Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return noteQueryService.find(new NoteCriteria(query, false, tags, color, pinned), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findDeleted(Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return noteQueryService.find(new NoteCriteria(query, true, tags, color, pinned), pageable);
    }

    @Transactional(readOnly = true)
    public NoteDTO findById(Long id) {
        var note = findActiveNote(id);
        return noteMapper.toDto(note);
    }

    @Transactional
    public void delete(Long id) {
        int updated = noteRepository.softDeleteById(id);
        if (updated == 0) {
            throw new NoteNotFoundException(id);
        }
    }

    @Transactional
    public void restore(Long id) {
        int updated = noteRepository.restoreById(id);
        if (updated == 0) {
            throw new NoteNotFoundException(id);
        }
    }

    @Transactional
    public void emptyTrash() {
        noteRepository.purgeDeleted();
        tagService.cleanupOrphanTagsAsync();
    }

    @Transactional
    public void deletePermanently(Long id) {
        var note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
        if (!note.isDeleted()) {
            throw new InvalidPermanentDeleteException(id);
        }
        noteRepository.deleteById(id);
        tagService.cleanupOrphanTagsAsync();
    }

    @Transactional
    public BulkActionResult bulk(BulkActionRequest request) {
        Set<Long> ids = request.ids();
        if (ids.isEmpty()) {
            return new BulkActionResult(0, List.of());
        }
        BulkAction action = BulkAction.valueOf(request.action());
        var failed = new ArrayList<Long>();
        var notesById = noteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));
        ids.stream()
                .filter(id -> !notesById.containsKey(id))
                .forEach(failed::add);

        int processed = switch (action) {
            case DELETE_SOFT -> {
                var toDelete = notesById.values().stream()
                        .filter(note -> !note.isDeleted())
                        .map(Note::getId)
                        .toList();
                notesById.values().stream()
                        .filter(Note::isDeleted)
                        .map(Note::getId)
                        .forEach(failed::add);
                yield toDelete.isEmpty() ? 0 : noteRepository.softDeleteByIds(toDelete);
            }
            case RESTORE -> {
                var toRestore = notesById.values().stream()
                        .filter(Note::isDeleted)
                        .map(Note::getId)
                        .toList();
                notesById.values().stream()
                        .filter(note -> !note.isDeleted())
                        .map(Note::getId)
                        .forEach(failed::add);
                yield toRestore.isEmpty() ? 0 : noteRepository.restoreByIds(toRestore);
            }
            case DELETE_FOREVER -> {
                var deletable = notesById.values().stream()
                        .filter(Note::isDeleted)
                        .map(Note::getId)
                        .toList();
                notesById.values().stream()
                        .filter(note -> !note.isDeleted())
                        .map(Note::getId)
                        .forEach(failed::add);
                if (!deletable.isEmpty()) {
                    noteRepository.deleteAllByIdInBatch(deletable);
                }
                yield deletable.size();
            }
        };

        return new BulkActionResult(processed, failed);
    }

    private Note findActiveNote(Long id) {
        return noteRepository.findOne(
                Specification.where(NoteSpecifications.isNotDeleted())
                    .and((root, cq, cb) -> cb.equal(root.get("id"), id)))
            .orElseThrow(() -> new NoteNotFoundException(id));
    }

}
