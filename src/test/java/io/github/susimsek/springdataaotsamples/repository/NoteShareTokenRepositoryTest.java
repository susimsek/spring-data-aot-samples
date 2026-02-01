package io.github.susimsek.springdataaotsamples.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.AuditingTestConfig;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import java.time.Instant;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest(
        properties = {
            "spring.liquibase.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@Import(AuditingTestConfig.class)
class NoteShareTokenRepositoryTest {

    @Autowired private NoteShareTokenRepository noteShareTokenRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void findAllWithNoteShouldLoadNotesAndKeepSortOrder() {
        Note firstNote = createNote("First Note", "Content 1", "alice");
        Note secondNote = createNote("Second Note", "Content 2", "bob");
        entityManager.persist(firstNote);
        entityManager.persist(secondNote);
        entityManager.flush();
        NoteShareToken firstToken = createShareToken(firstNote, "token1hash", SharePermission.READ);
        NoteShareToken secondToken =
                createShareToken(secondNote, "token2hash", SharePermission.READ);
        entityManager.persist(firstToken);
        entityManager.persist(secondToken);
        entityManager.flush();
        entityManager.getEntityManager().clear();
        Specification<NoteShareToken> spec = (root, query, cb) -> null;
        Page<NoteShareToken> page =
                noteShareTokenRepository.findAllWithNote(
                        spec, PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id"))));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getNote()).isNotNull();
        assertThat(page.getContent().get(0).getNote().getTitle()).isEqualTo("Second Note");
        assertThat(page.getContent().get(1).getNote()).isNotNull();
        assertThat(page.getContent().get(1).getNote().getTitle()).isEqualTo("First Note");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAllWithNoteShouldHandleEmptyResults() {
        Specification<NoteShareToken> spec = (root, query, cb) -> cb.equal(root.get("id"), -999L);
        Page<NoteShareToken> page =
                noteShareTokenRepository.findAllWithNote(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void findAllWithNoteShouldApplyPagination() {
        for (int i = 0; i < 5; i++) {
            Note note = createNote("Note " + i, "Content " + i, "user" + i);
            entityManager.persist(note);
            NoteShareToken token = createShareToken(note, "tokenhash" + i, SharePermission.READ);
            entityManager.persist(token);
        }
        entityManager.flush();
        entityManager.getEntityManager().clear();
        Specification<NoteShareToken> spec = (root, query, cb) -> null;
        Page<NoteShareToken> firstPage =
                noteShareTokenRepository.findAllWithNote(
                        spec, PageRequest.of(0, 2, Sort.by(Sort.Order.asc("id"))));
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
        Page<NoteShareToken> secondPage =
                noteShareTokenRepository.findAllWithNote(
                        spec, PageRequest.of(1, 2, Sort.by(Sort.Order.asc("id"))));
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getTotalElements()).isEqualTo(5);
        assertThat(secondPage.hasNext()).isTrue();
        Page<NoteShareToken> lastPage =
                noteShareTokenRepository.findAllWithNote(
                        spec, PageRequest.of(2, 2, Sort.by(Sort.Order.asc("id"))));
        assertThat(lastPage.getContent()).hasSize(1);
        assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    void findAllWithNoteShouldApplySpecification() {
        Note note1 = createNote("Note 1", "Content 1", "alice");
        Note note2 = createNote("Note 2", "Content 2", "bob");
        entityManager.persist(note1);
        entityManager.persist(note2);
        entityManager.flush();
        NoteShareToken token1 = createShareToken(note1, "hash1", SharePermission.READ);
        token1.setRevoked(false);
        NoteShareToken token2 = createShareToken(note2, "hash2", SharePermission.READ);
        token2.setRevoked(true);
        entityManager.persist(token1);
        entityManager.persist(token2);
        entityManager.flush();
        entityManager.getEntityManager().clear();
        Specification<NoteShareToken> spec =
                (root, query, cb) -> cb.equal(root.get("revoked"), false);
        Page<NoteShareToken> page =
                noteShareTokenRepository.findAllWithNote(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().isRevoked()).isFalse();
        assertThat(page.getContent().getFirst().getNote().getTitle()).isEqualTo("Note 1");
    }

    @Test
    void findAllWithNoteShouldEagerLoadNote() {
        Note note = createNote("Test Note", "Content", "alice");
        entityManager.persist(note);
        entityManager.flush();
        NoteShareToken token = createShareToken(note, "testhash", SharePermission.READ);
        entityManager.persist(token);
        entityManager.flush();
        entityManager.getEntityManager().clear();
        Specification<NoteShareToken> spec = (root, query, cb) -> null;
        Page<NoteShareToken> page =
                noteShareTokenRepository.findAllWithNote(spec, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        NoteShareToken loadedToken = page.getContent().getFirst();
        assertThat(loadedToken.getNote()).isNotNull();
        assertThat(loadedToken.getNote().getTitle()).isEqualTo("Test Note");
        assertThat(loadedToken.getNote().getContent()).isEqualTo("Content");
    }

    private Note createNote(String title, String content, String owner) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setOwner(owner);
        note.setPinned(false);
        note.setColor("#FFFFFF");
        note.setVersion(0L);
        note.setTags(new LinkedHashSet<>());
        return note;
    }

    private NoteShareToken createShareToken(
            Note note, String tokenHash, SharePermission permission) {
        NoteShareToken token = new NoteShareToken();
        token.setNote(note);
        token.setTokenHash(tokenHash);
        token.setPermission(permission);
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(86400));
        return token;
    }
}
