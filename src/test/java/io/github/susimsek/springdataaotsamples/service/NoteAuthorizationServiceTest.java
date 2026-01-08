package io.github.susimsek.springdataaotsamples.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class NoteAuthorizationServiceTest {

    private final NoteAuthorizationService service = new NoteAuthorizationService();

    @Test
    void ensureReadAccessShouldAllowAdmin() {
        Note note = new Note();
        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);

            assertThatCode(() -> service.ensureReadAccess(note)).doesNotThrowAnyException();

            utils.verify(SecurityUtils::isCurrentUserAdmin);
            utils.verifyNoMoreInteractions();
        }
    }

    @Test
    void ensureEditAccessShouldThrowWhenUserMissing() {
        Note note = new Note();
        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.ensureEditAccess(note))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Test
    void ensureEditAccessShouldThrowWhenNotOwner() {
        Note note = new Note();
        note.setOwner("bob");
        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));

            assertThatThrownBy(() -> service.ensureEditAccess(note))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void ensureEditAccessShouldPassWhenOwnerMatches() {
        Note note = new Note();
        note.setOwner("alice");
        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));

            assertThatCode(() -> service.ensureEditAccess(note)).doesNotThrowAnyException();
            utils.verify(SecurityUtils::getCurrentUserLogin);
        }
    }
}
