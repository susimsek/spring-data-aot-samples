package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.command.TagCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.query.NoteQueryService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteTrashService {

    private final NoteRepository noteRepository;
    private final TagCommandService tagCommandService;
    private final NoteAuthorizationService noteAuthorizationService;
    private final NoteQueryService noteQueryService;
    private final CacheProvider cacheProvider;

    @Transactional(readOnly = true)
    public Page<NoteDTO> findDeleted(
            Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return noteQueryService.findByCriteria(
                new NoteCriteria(query, true, tags, color, pinned, null), pageable);
    }

    @Transactional(readOnly = true)
    public Page<NoteDTO> findDeletedForCurrentUser(
            Pageable pageable, String query, Set<String> tags, String color, Boolean pinned) {
        return noteQueryService.findByCriteria(
                new NoteCriteria(query, true, tags, color, pinned, getCurrentUsername()), pageable);
    }

    @Transactional
    public void restore(Long id) {
        int updated = noteRepository.restoreById(id);
        if (updated == 0) {
            throw new NoteNotFoundException(id);
        }
        evictNoteCaches();
    }

    @Transactional
    public void restoreForCurrentUser(Long id) {
        var note = noteRepository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
        noteAuthorizationService.ensureEditAccess(note);
        if (!note.isDeleted()) {
            throw new NoteNotFoundException(id);
        }
        int updated = noteRepository.restoreById(id);
        if (updated == 0) {
            throw new NoteNotFoundException(id);
        }
        evictNoteCaches();
    }

    @Transactional
    public void emptyTrash() {
        noteRepository.purgeDeleted();
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCaches();
    }

    @Transactional
    public void emptyTrashForCurrentUser() {
        var username =
                SecurityUtils.getCurrentUserLogin()
                        .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        noteRepository.purgeDeletedByOwner(username);
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCaches();
    }

    @Transactional
    public void deletePermanently(Long id) {
        var note = noteRepository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
        if (!note.isDeleted()) {
            throw new InvalidPermanentDeleteException(id);
        }
        noteRepository.deleteById(id);
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCaches();
    }

    @Transactional
    public void deletePermanentlyForCurrentUser(Long id) {
        var note = noteRepository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
        noteAuthorizationService.ensureEditAccess(note);
        if (!note.isDeleted()) {
            throw new InvalidPermanentDeleteException(id);
        }
        noteRepository.deleteById(id);
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCaches();
    }

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }

    private void evictNoteCaches() {
        cacheProvider.clearCaches(
                Note.class.getName(), Note.class.getName() + ".tags", Tag.class.getName());
    }
}
