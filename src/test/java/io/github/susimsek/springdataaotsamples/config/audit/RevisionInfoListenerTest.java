package io.github.susimsek.springdataaotsamples.config.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

class RevisionInfoListenerTest {

    @Test
    void newRevisionShouldPopulateUsernameFromAuditor() {
        RevisionInfoListener listener = new RevisionInfoListener();
        @SuppressWarnings("unchecked")
        AuditorAware<String> auditorAware = mock(AuditorAware.class);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("alice"));
        listener.setAuditorAware(auditorAware);

        RevisionInfo revisionInfo = new RevisionInfo();

        listener.newRevision(revisionInfo);

        assertThat(revisionInfo.getUsername()).isEqualTo("alice");
    }

    @Test
    void newRevisionShouldIgnoreNonRevisionInfo() {
        RevisionInfoListener listener = new RevisionInfoListener();
        listener.newRevision(new Object()); // should not throw
    }
}
