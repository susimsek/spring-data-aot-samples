package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.enumeration.BulkAction;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionRequest;
import io.github.susimsek.springdataaotsamples.service.dto.BulkActionResult;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.exception.RevisionNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteRevisionMapper;
import io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final NoteQueryService noteQueryService;
    private final NoteMapper noteMapper;
    private final NoteRevisionMapper noteRevisionMapper;

    @Transactional
    public NoteDTO create(NoteCreateRequest request) {
        var note = noteMapper.toEntity(request);
        note.setTags(resolveTags(request.tags()));
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO update(Long id, NoteUpdateRequest request) {
        var note = findActiveNote(id);
        noteMapper.updateEntity(request, note);
        note.setTags(resolveTags(request.tags()));
        var saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDTO patch(Long id, NotePatchRequest request) {
        var note = findActiveNote(id);
        noteMapper.patchEntity(request, note);
        if (request.tags() != null) {
            note.setTags(resolveTags(request.tags()));
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
    public Page<NoteDTO> findAll(Pageable pageable, String query) {
        return noteQueryService.find(new NoteCriteria(query, false), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findDeleted(Pageable pageable, String query) {
        return noteQueryService.find(new NoteCriteria(query, true), pageable);
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
    }

    @Transactional
    public void deletePermanently(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new NoteNotFoundException(id);
        }
        noteRepository.deleteById(id);
    }

    @Transactional
    public BulkActionResult bulk(BulkActionRequest request) {
        Set<Long> ids = new HashSet<>(request.ids());
        if (ids.isEmpty()) {
            return new BulkActionResult(0, List.of());
        }
        BulkAction action = BulkAction.valueOf(request.action());
        int processed = switch (action) {
            case DELETE_SOFT -> noteRepository.softDeleteByIds(List.copyOf(ids));
            case RESTORE -> noteRepository.restoreByIds(List.copyOf(ids));
            case DELETE_FOREVER -> {
                noteRepository.deleteAllByIdInBatch(ids);
                yield ids.size();
            }
        };

        return new BulkActionResult(processed, List.of());
    }

    private Note findActiveNote(Long id) {
        return noteRepository.findOne(
                Specification.where(NoteSpecifications.isNotDeleted())
                    .and((root, cq, cb) -> cb.equal(root.get("id"), id)))
            .orElseThrow(() -> new NoteNotFoundException(id));
    }

    private Set<Tag> resolveTags(Set<String> tagNames) {
        if (tagNames.isEmpty()) {
            return new LinkedHashSet<>();
        }
        var normalized = tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return new LinkedHashSet<>();
        }

        var existing = tagRepository.findByNameIn(normalized);
        Map<String, Tag> byName = existing.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        var missing = normalized.stream()
                .filter(name -> !byName.containsKey(name))
                .map(name -> {
                    var tag = new Tag();
                    tag.setName(name);
                    return tag;
                })
                .toList();

        if (!missing.isEmpty()) {
            var saved = tagRepository.saveAll(missing);
            saved.forEach(tag -> byName.put(tag.getName(), tag));
        }

        return new LinkedHashSet<>(byName.values());
    }
}
