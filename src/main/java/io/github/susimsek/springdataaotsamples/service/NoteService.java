package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.enumeration.BulkAction;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.RevisionNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.UserNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final UserRepository userRepository;
    private final NoteAuthorizationService noteAuthorizationService;

    @Transactional
    public NoteDTO create(NoteCreateRequest request) {
        var note = noteMapper.toEntity(request);
        var username = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        note.setOwner(username);
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
    public NoteDTO updateForCurrentUser(Long id, NoteUpdateRequest request) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureOwner(note);
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

    @Transactional
    public NoteDTO patchForCurrentUser(Long id, NotePatchRequest request) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureOwner(note);
        noteMapper.patchEntity(request, note);
        if (request.tags() != null) {
            note.setTags(tagService.resolveTags(request.tags()));
        }
        // owner change ignored for non-admin path
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO changeOwner(Long id, String owner) {
        if (!userRepository.existsByUsername(owner)) {
            throw new UserNotFoundException(owner);
        }
        var note = findActiveNote(id);
        note.setOwner(owner);
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO restoreRevision(Long id, Long revisionNumber) {
        var note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
        var revision = noteRepository.findRevision(id, revisionNumber)
                .orElseThrow(() -> new RevisionNotFoundException(id, revisionNumber));
        var snapshot = revision.getEntity();
        noteMapper.applyRevision(snapshot, note);

        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO restoreRevisionForCurrentUser(Long id, Long revisionNumber) {
        var note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
        var revision = noteRepository.findRevision(id, revisionNumber)
                .orElseThrow(() -> new RevisionNotFoundException(id, revisionNumber));
        var snapshot = revision.getEntity();
        noteAuthorizationService.ensureOwner(note);

        noteMapper.applyRevision(snapshot, note);

        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findAll(Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return noteQueryService.find(new NoteCriteria(query, false, tags, color, pinned, null), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findDeleted(Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return noteQueryService.find(new NoteCriteria(query, true, tags, color, pinned, null), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findAllForCurrentUser(Pageable pageable,
                                               String query, Set<String> tags,
                                               String color, Boolean pinned) {
        var username = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        return noteQueryService.find(new NoteCriteria(query, false, tags, color, pinned, username), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findDeletedForCurrentUser(Pageable pageable,
                                                   String query, Set<String> tags,
                                                   String color, Boolean pinned) {
        var username = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        return noteQueryService.find(new NoteCriteria(query, true, tags, color, pinned, username), pageable);
    }

    @Transactional(readOnly = true)
    public NoteDTO findById(Long id) {
        var note = findActiveNote(id);
        return noteMapper.toDto(note);
    }

    @Transactional(readOnly = true)
    public NoteDTO findByIdForCurrentUser(Long id) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureOwner(note);
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
    public void deleteForCurrentUser(Long id) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureOwner(note);
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
    public void restoreForCurrentUser(Long id) {
        var note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
        noteAuthorizationService.ensureOwner(note);
        if (!note.isDeleted()) {
            throw new NoteNotFoundException(id);
        }
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
    public void emptyTrashForCurrentUser() {
        var username = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        noteRepository.purgeDeletedByOwner(username);
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
    public void deletePermanentlyForCurrentUser(Long id) {
        var note = noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
        noteAuthorizationService.ensureOwner(note);
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
        var notesById = noteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));
        var failed = ids.stream()
                .filter(id -> !notesById.containsKey(id))
                .collect(Collectors.toCollection(ArrayList::new));
        return executeBulk(action, notesById, failed);
    }

    @Transactional
    public BulkActionResult bulkForCurrentUser(BulkActionRequest request) {
        Set<Long> ids = request.ids();
        if (ids.isEmpty()) {
            return new BulkActionResult(0, List.of());
        }
        BulkAction action = BulkAction.valueOf(request.action());
        var notesById = noteRepository.findAllByIdInForCurrentUser(ids).stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));
        // missing IDs are treated as not found; add to failed
        var failed = ids.stream()
                .filter(id -> !notesById.containsKey(id))
                .collect(Collectors.toCollection(ArrayList::new));
        return executeBulk(action, notesById, failed);
    }

    private Note findActiveNote(Long id) {
        return noteRepository.findOne(
                Specification.where(NoteSpecifications.isNotDeleted())
                        .and((root, cq, cb) -> cb.equal(root.get("id"), id)))
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    private Note findNote(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    private BulkActionResult executeBulk(BulkAction action, Map<Long, Note> notesById, List<Long> failed) {
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
}
