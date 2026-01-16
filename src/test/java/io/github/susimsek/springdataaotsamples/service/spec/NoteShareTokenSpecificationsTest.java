package io.github.susimsek.springdataaotsamples.service.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.AuditableEntity_;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken_;
import io.github.susimsek.springdataaotsamples.domain.Note_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteShareTokenSpecificationsTest {

    @Mock private Root<NoteShareToken> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder cb;

    @Mock private Predicate predicate;

    @Test
    void ownedByShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteShareTokenSpecifications.ownedBy(" ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void ownedByShouldMatchOwner() {
        ensureJpaMetamodelInitialized();
        Path<Note> notePath = mock(Path.class);
        Path<String> ownerPath = mock(Path.class);
        Predicate eq = mock(Predicate.class);

        when(root.get(NoteShareToken_.note)).thenReturn(notePath);
        when(notePath.get(Note_.owner)).thenReturn(ownerPath);
        when(cb.equal(ownerPath, "alice")).thenReturn(eq);

        Predicate result =
                NoteShareTokenSpecifications.ownedBy("alice").toPredicate(root, query, cb);

        assertThat(result).isSameAs(eq);
    }

    @Test
    void forNoteShouldReturnConjunctionWhenNull() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteShareTokenSpecifications.forNote(null).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void forNoteShouldMatchNoteId() {
        ensureJpaMetamodelInitialized();
        Path<Note> notePath = mock(Path.class);
        Path<Long> idPath = mock(Path.class);
        Predicate eq = mock(Predicate.class);

        when(root.get(NoteShareToken_.note)).thenReturn(notePath);
        when(notePath.get(Note_.id)).thenReturn(idPath);
        when(cb.equal(idPath, 5L)).thenReturn(eq);

        Predicate result = NoteShareTokenSpecifications.forNote(5L).toPredicate(root, query, cb);

        assertThat(result).isSameAs(eq);
    }

    @Test
    void searchShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteShareTokenSpecifications.search(" ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void searchShouldMatchTokenOrTitle() {
        ensureJpaMetamodelInitialized();
        Path<String> tokenHash = mock(Path.class);
        Path<Note> notePath = mock(Path.class);
        Path<String> title = mock(Path.class);
        Expression<String> lowered = mock(Expression.class);
        Predicate tokenLike = mock(Predicate.class);
        Predicate titleLike = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);

        when(root.get(NoteShareToken_.tokenHash)).thenReturn(tokenHash);
        when(root.get(NoteShareToken_.note)).thenReturn(notePath);
        when(notePath.get(Note_.title)).thenReturn(title);
        when(cb.lower(title)).thenReturn(lowered);
        when(cb.like(tokenHash, "%abc%")).thenReturn(tokenLike);
        when(cb.like(lowered, "%abc%")).thenReturn(titleLike);
        when(cb.or(tokenLike, titleLike)).thenReturn(orPredicate);

        Predicate result = NoteShareTokenSpecifications.search("Abc").toPredicate(root, query, cb);

        assertThat(result).isSameAs(orPredicate);
        verify(cb).or(tokenLike, titleLike);
    }

    private static void ensureJpaMetamodelInitialized() {
        NoteShareToken_.tokenHash = namedStringAttribute("tokenHash");
        NoteShareToken_.note = namedNoteAttribute("note");
        NoteShareToken_.revoked = namedBooleanAttribute("revoked");
        NoteShareToken_.expiresAt = namedInstantAttribute("expiresAt");

        Note_.title = namedStringAttribute("title");
        Note_.owner = namedStringAttribute("owner");
        Note_.id = namedLongAttribute("id");

        AuditableEntity_.createdDate = namedInstantAttribute("createdDate");
    }

    @SuppressWarnings("unchecked")
    private static <T> SingularAttribute<T, String> namedStringAttribute(String name) {
        SingularAttribute<T, String> attribute = mock(SingularAttribute.class);
        return attribute;
    }

    @SuppressWarnings("unchecked")
    private static <T> SingularAttribute<T, Long> namedLongAttribute(String name) {
        SingularAttribute<T, Long> attribute = mock(SingularAttribute.class);
        return attribute;
    }

    @SuppressWarnings("unchecked")
    private static <T> SingularAttribute<T, Boolean> namedBooleanAttribute(String name) {
        SingularAttribute<T, Boolean> attribute = mock(SingularAttribute.class);
        return attribute;
    }

    @SuppressWarnings("unchecked")
    private static <T> SingularAttribute<T, Instant> namedInstantAttribute(String name) {
        SingularAttribute<T, Instant> attribute = mock(SingularAttribute.class);
        return attribute;
    }

    @SuppressWarnings("unchecked")
    private static SingularAttribute<NoteShareToken, Note> namedNoteAttribute(String name) {
        SingularAttribute<NoteShareToken, Note> attribute = mock(SingularAttribute.class);
        return attribute;
    }

    @Test
    void statusShouldReturnConjunctionWhenAllOrBlank() {
        when(cb.conjunction()).thenReturn(predicate);

        assertThat(NoteShareTokenSpecifications.status(null).toPredicate(root, query, cb))
                .isSameAs(predicate);
        assertThat(NoteShareTokenSpecifications.status("all").toPredicate(root, query, cb))
                .isSameAs(predicate);
    }

    @Test
    void statusShouldHandleRevoked() {
        ensureJpaMetamodelInitialized();
        Path<Boolean> revoked = mock(Path.class);
        Predicate isTrue = mock(Predicate.class);
        when(root.get(NoteShareToken_.revoked)).thenReturn(revoked);
        when(cb.isTrue(revoked)).thenReturn(isTrue);

        Predicate result =
                NoteShareTokenSpecifications.status("revoked").toPredicate(root, query, cb);

        assertThat(result).isSameAs(isTrue);
    }

    @Test
    void statusShouldHandleExpired() {
        ensureJpaMetamodelInitialized();
        Path<Instant> expires = mock(Path.class);
        Predicate notNull = mock(Predicate.class);
        Predicate beforeNow = mock(Predicate.class);
        Predicate andPredicate = mock(Predicate.class);
        Instant now = Instant.now();

        when(root.get(NoteShareToken_.expiresAt)).thenReturn(expires);
        when(cb.isNotNull(expires)).thenReturn(notNull);
        when(cb.lessThan(expires, now)).thenReturn(beforeNow);
        when(cb.and(notNull, beforeNow)).thenReturn(andPredicate);

        try (var mocked = mockStatic(Instant.class)) {
            mocked.when(Instant::now).thenReturn(now);

            Predicate result =
                    NoteShareTokenSpecifications.status("expired").toPredicate(root, query, cb);

            assertThat(result).isSameAs(andPredicate);
        }
    }

    @Test
    void statusShouldHandleActive() {
        ensureJpaMetamodelInitialized();
        Path<Boolean> revoked = mock(Path.class);
        Path<Instant> expires = mock(Path.class);
        Predicate revokedFalse = mock(Predicate.class);
        Predicate nullExpires = mock(Predicate.class);
        Predicate futureExpires = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);
        Predicate andPredicate = mock(Predicate.class);
        Instant now = Instant.now();

        when(root.get(NoteShareToken_.revoked)).thenReturn(revoked);
        when(cb.isFalse(revoked)).thenReturn(revokedFalse);
        when(root.get(NoteShareToken_.expiresAt)).thenReturn(expires);
        when(cb.isNull(expires)).thenReturn(nullExpires);
        when(cb.greaterThan(expires, now)).thenReturn(futureExpires);
        when(cb.or(nullExpires, futureExpires)).thenReturn(orPredicate);
        when(cb.and(revokedFalse, orPredicate)).thenReturn(andPredicate);

        try (var mocked = mockStatic(Instant.class)) {
            mocked.when(Instant::now).thenReturn(now);

            Predicate result =
                    NoteShareTokenSpecifications.status("active").toPredicate(root, query, cb);

            assertThat(result).isSameAs(andPredicate);
        }
    }

    @Test
    void createdBetweenShouldReturnConjunctionWhenBothNull() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result =
                NoteShareTokenSpecifications.createdBetween(null, null)
                        .toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void createdBetweenShouldHandleFromAndTo() {
        ensureJpaMetamodelInitialized();
        Predicate conj = mock(Predicate.class);
        Predicate gte = mock(Predicate.class);
        Predicate lte = mock(Predicate.class);
        Predicate and = mock(Predicate.class);
        Path<Instant> createdDate = mock(Path.class);

        when(cb.conjunction()).thenReturn(conj);
        when(root.get(AuditableEntity_.createdDate)).thenReturn(createdDate);
        when(cb.greaterThanOrEqualTo(createdDate, Instant.EPOCH)).thenReturn(gte);
        when(cb.and(conj, gte)).thenReturn(gte);
        when(cb.lessThanOrEqualTo(createdDate, Instant.MAX)).thenReturn(lte);
        when(cb.and(gte, lte)).thenReturn(and);

        Predicate result =
                NoteShareTokenSpecifications.createdBetween(Instant.EPOCH, Instant.MAX)
                        .toPredicate(root, query, cb);

        assertThat(result).isSameAs(and);
    }
}
