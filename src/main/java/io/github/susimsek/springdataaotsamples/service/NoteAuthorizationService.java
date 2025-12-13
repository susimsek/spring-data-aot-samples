package io.github.susimsek.springdataaotsamples.service;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class NoteAuthorizationService {

    public void ensureReadAccess(Note note) {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return;
        }
        ensureOwner(note);
    }

    public void ensureEditAccess(Note note) {
        ensureOwner(note);
    }

    private void ensureOwner(Note note) {
        var username = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
        var owner = note.getOwner();
        if (!username.equals(owner)) {
            throw new AccessDeniedException("You are not allowed to modify this note");
        }
    }

}
