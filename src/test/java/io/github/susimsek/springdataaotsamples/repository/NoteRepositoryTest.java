package io.github.susimsek.springdataaotsamples.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.AuditingTestConfig;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.service.spec.NoteSpecifications;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DataJpaTest(
        properties = {
            "spring.liquibase.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@Import(AuditingTestConfig.class)
class NoteRepositoryTest {

    @Autowired private NoteRepository noteRepository;

    @Autowired private TestEntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAllWithTagsShouldLoadTagsAndKeepSortOrder() {
        Tag javaTag = new Tag(null, "java");
        Tag springTag = new Tag(null, "spring");
        entityManager.persist(javaTag);
        entityManager.persist(springTag);

        Note first = new Note();
        first.setTitle("A title");
        first.setContent("A content");
        first.setOwner("alice");
        first.setPinned(false);
        first.setColor("#111111");
        first.setVersion(0L);
        first.setTags(new LinkedHashSet<>(Set.of(javaTag)));

        Note second = new Note();
        second.setTitle("B title");
        second.setContent("B content");
        second.setOwner("alice");
        second.setPinned(false);
        second.setColor("#222222");
        second.setVersion(0L);
        second.setTags(new LinkedHashSet<>(Set.of(springTag)));

        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();
        entityManager.getEntityManager().clear();

        Specification<Note> spec = Specification.where(NoteSpecifications.isNotDeleted());
        var page =
                noteRepository.findAllWithTags(
                        spec, PageRequest.of(0, 10, Sort.by(Sort.Order.desc("title"))));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("B title");
        assertThat(page.getContent().get(0).getTags()).isNotEmpty();
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("A title");
        assertThat(page.getContent().get(1).getTags()).isNotEmpty();
    }

    @Test
    void findAllByIdInForCurrentUserShouldFilterByAuthenticationName() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a"));

        Note aliceNote = new Note();
        aliceNote.setTitle("Alice note");
        aliceNote.setContent("Alice content");
        aliceNote.setOwner("alice");
        aliceNote.setVersion(0L);

        Note bobNote = new Note();
        bobNote.setTitle("Bob note");
        bobNote.setContent("Bob content");
        bobNote.setOwner("bob");
        bobNote.setVersion(0L);

        entityManager.persist(aliceNote);
        entityManager.persist(bobNote);
        entityManager.flush();
        entityManager.getEntityManager().clear();
        Long aliceId = aliceNote.getId();
        Long bobId = bobNote.getId();

        List<Note> found = noteRepository.findAllByIdInForCurrentUser(Set.of(aliceId, bobId));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOwner()).isEqualTo("alice");
    }

    @Test
    void softDeleteAndRestoreShouldUpdateDeletedFields() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a"));

        Note note = new Note();
        note.setTitle("To delete");
        note.setContent("Content");
        note.setOwner("alice");
        note.setVersion(0L);
        entityManager.persist(note);
        entityManager.flush();

        int deleted = noteRepository.softDeleteById(note.getId());
        assertThat(deleted).isEqualTo(1);

        entityManager.getEntityManager().clear();
        Note deletedNote = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(deletedNote.isDeleted()).isTrue();
        assertThat(deletedNote.getDeletedBy()).isEqualTo("alice");
        assertThat(deletedNote.getDeletedDate()).isNotNull();

        int restored = noteRepository.restoreById(note.getId());
        assertThat(restored).isEqualTo(1);

        entityManager.getEntityManager().clear();
        Note restoredNote = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(restoredNote.isDeleted()).isFalse();
        assertThat(restoredNote.getDeletedBy()).isNull();
        assertThat(restoredNote.getDeletedDate()).isNull();
    }
}
