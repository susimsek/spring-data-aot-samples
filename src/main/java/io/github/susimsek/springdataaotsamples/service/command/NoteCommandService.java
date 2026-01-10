package io.github.susimsek.springdataaotsamples.service.command;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.enumeration.BulkAction;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.NoteAuthorizationService;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.UserNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteCommandService {

    private final NoteRepository noteRepository;
    private final TagCommandService tagCommandService;
    private final NoteMapper noteMapper;
    private final UserRepository userRepository;
    private final NoteAuthorizationService noteAuthorizationService;
    private final CacheProvider cacheProvider;

    @Transactional
    public NoteDTO create(NoteCreateRequest request) {
        var note = noteMapper.toEntity(request);
        var username =
                SecurityUtils.getCurrentUserLogin()
                        .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        note.setOwner(username);
        applyTags(note, request.tags());
        return save(note);
    }

    @Transactional
    public NoteDTO update(Long id, NoteUpdateRequest request) {
        var note = findActiveNote(id);
        return applyUpdate(note, request);
    }

    @Transactional
    public NoteDTO updateForCurrentUser(Long id, NoteUpdateRequest request) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureEditAccess(note);
        return applyUpdate(note, request);
    }

    @Transactional
    public NoteDTO patch(Long id, NotePatchRequest request) {
        var note = findActiveNote(id);
        return applyPatch(note, request);
    }

    @Transactional
    public NoteDTO patchForCurrentUser(Long id, NotePatchRequest request) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureEditAccess(note);
        // owner change ignored for non-admin path
        return applyPatch(note, request);
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
    public void delete(Long id) {
        int updated = noteRepository.softDeleteById(id);
        if (updated == 0) {
            throw new NoteNotFoundException(id);
        }
        evictNoteCaches();
    }

    @Transactional
    public void deleteForCurrentUser(Long id) {
        var note = findActiveNote(id);
        noteAuthorizationService.ensureEditAccess(note);
        int updated = noteRepository.softDeleteById(id);
        if (updated == 0) {
            throw new NoteNotFoundException(id);
        }
        evictNoteCaches();
    }

    @Transactional
    public BulkActionResult bulk(BulkActionRequest request) {
        Set<Long> ids = request.ids();
        if (ids.isEmpty()) {
            return new BulkActionResult(0, List.of());
        }
        BulkAction action = BulkAction.valueOf(request.action());
        var notesById =
                noteRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Note::getId, Function.identity()));
        var failed =
                ids.stream()
                        .filter(id -> !notesById.containsKey(id))
                        .collect(Collectors.toCollection(ArrayList::new));
        var result = executeBulk(action, notesById, failed);
        if (result.processedCount() > 0) {
            evictNoteCaches();
        }
        return result;
    }

    @Transactional
    public BulkActionResult bulkForCurrentUser(BulkActionRequest request) {
        Set<Long> ids = request.ids();
        if (ids.isEmpty()) {
            return new BulkActionResult(0, List.of());
        }
        BulkAction action = BulkAction.valueOf(request.action());
        var notesById =
                noteRepository.findAllByIdInForCurrentUser(ids).stream()
                        .collect(Collectors.toMap(Note::getId, Function.identity()));
        // missing IDs are treated as not found; add to failed
        var failed =
                ids.stream()
                        .filter(id -> !notesById.containsKey(id))
                        .collect(Collectors.toCollection(ArrayList::new));
        var result = executeBulk(action, notesById, failed);
        if (result.processedCount() > 0) {
            evictNoteCaches();
        }
        return result;
    }

    private Note findActiveNote(Long id) {
        return noteRepository
                .findOne(
                        Specification.where(NoteSpecifications.isNotDeleted())
                                .and(
                                        (root, cq, cb) ->
                                                cb.equal(
                                                        root.get(
                                                                io.github.susimsek
                                                                        .springdataaotsamples.domain
                                                                        .Note_.id),
                                                        id)))
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    private void applyTags(Note note, @Nullable Set<String> tags) {
        note.setTags(tagCommandService.resolveTags(tags));
    }

    private NoteDTO applyUpdate(Note note, NoteUpdateRequest request) {
        noteMapper.updateEntity(request, note);
        applyTags(note, request.tags());
        return save(note);
    }

    private NoteDTO applyPatch(Note note, NotePatchRequest request) {
        noteMapper.patchEntity(request, note);
        if (request.tags() != null) {
            applyTags(note, request.tags());
        }
        return save(note);
    }

    private NoteDTO save(Note note) {
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    private void evictNoteCaches() {
        cacheProvider.clearCaches(Note.class.getName(), Note.class.getName() + ".tags");
    }

    private BulkActionResult executeBulk(
            BulkAction action, Map<Long, Note> notesById, List<Long> failed) {
        int processed =
                switch (action) {
                    case DELETE_SOFT -> {
                        var toDelete =
                                notesById.values().stream()
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
                        var toRestore =
                                notesById.values().stream()
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
                        var deletable =
                                notesById.values().stream()
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
