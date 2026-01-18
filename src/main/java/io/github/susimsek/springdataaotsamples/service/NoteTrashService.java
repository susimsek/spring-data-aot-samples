package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.command.TagCommandService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.query.NoteQueryService;
import java.util.List;
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
        evictNoteCachesById(id);
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
        evictNoteCachesById(id);
    }

    @Transactional
    public void emptyTrash() {
        List<Long> deletedIds = noteRepository.findDeletedIds();
        noteRepository.purgeDeleted();
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCachesByIds(deletedIds);
    }

    @Transactional
    public void emptyTrashForCurrentUser() {
        var username =
                SecurityUtils.getCurrentUserLogin()
                        .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        List<Long> deletedIds = noteRepository.findDeletedIdsByOwner(username);
        noteRepository.purgeDeletedByOwner(username);
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCachesByIds(deletedIds);
    }

    @Transactional
    public void deletePermanently(Long id) {
        int deleted = noteRepository.deletePermanentlyById(id);
        if (deleted == 0) {
            Boolean deletedFlag = noteRepository.findDeletedFlagById(id).orElse(null);
            if (Boolean.FALSE.equals(deletedFlag)) {
                throw new InvalidPermanentDeleteException(id);
            }
            throw new NoteNotFoundException(id);
        }
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCachesById(id);
    }

    @Transactional
    public void deletePermanentlyForCurrentUser(Long id) {
        int deleted = noteRepository.deletePermanentlyByIdForCurrentUser(id);
        if (deleted == 0) {
            Boolean deletedFlag = noteRepository.findDeletedFlagByIdForCurrentUser(id).orElse(null);
            if (Boolean.FALSE.equals(deletedFlag)) {
                throw new InvalidPermanentDeleteException(id);
            }
            throw new NoteNotFoundException(id);
        }
        tagCommandService.cleanupOrphanTagsAsync();
        evictNoteCachesById(id);
    }

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }

    private void evictNoteCachesById(Long noteId) {
        cacheProvider.clearCache(Note.class.getName(), noteId);
        cacheProvider.clearCache(NoteRepository.NOTE_BY_ID_CACHE, noteId);
    }

    private void evictNoteCachesByIds(List<Long> noteIds) {
        cacheProvider.clearCache(Note.class.getName(), noteIds);
        cacheProvider.clearCache(NoteRepository.NOTE_BY_ID_CACHE, noteIds);
    }
}
