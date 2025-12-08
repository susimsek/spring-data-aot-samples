package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPermanentDeleteException;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteTrashService {

    private final NoteRepository noteRepository;
    private final TagService tagService;
    private final NoteAuthorizationService noteAuthorizationService;

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
}
