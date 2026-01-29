package io.github.susimsek.springdataaotsamples.service.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.AuditableEntity_;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Note_;
import io.github.susimsek.springdataaotsamples.domain.SoftDeletableEntity_;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.Tag_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@ResourceLock(value = "jpa-metamodel", mode = ResourceAccessMode.READ_WRITE)
class NoteSpecificationsTest {

    @Mock private Root<Note> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder cb;

    @Mock private Predicate predicate;

    @Test
    void isNotDeletedShouldReturnFalseCheck() {
        Path<Boolean> deleted = mock(Path.class);
        when(root.get(SoftDeletableEntity_.deleted)).thenReturn(deleted);
        when(cb.isFalse(deleted)).thenReturn(predicate);

        Predicate result = NoteSpecifications.isNotDeleted().toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).isFalse(deleted);
    }

    @Test
    void isDeletedShouldReturnTrueCheck() {
        Path<Boolean> deleted = mock(Path.class);
        when(root.get(SoftDeletableEntity_.deleted)).thenReturn(deleted);
        when(cb.isTrue(deleted)).thenReturn(predicate);

        Predicate result = NoteSpecifications.isDeleted().toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).isTrue(deleted);
    }

    @Test
    void searchShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteSpecifications.search(" ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).conjunction();
    }

    @Test
    void searchShouldMatchTitleOrContent() {
        Path<String> title = mock(Path.class);
        Path<String> content = mock(Path.class);
        Expression<String> lowered = mock(Expression.class);

        when(root.get(Note_.title)).thenReturn(title);
        when(root.get(Note_.content)).thenReturn(content);
        when(cb.lower(any(Path.class))).thenReturn(lowered);
        when(cb.like(any(Expression.class), any(String.class))).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);

        Predicate result = NoteSpecifications.search("java").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).or(any(Predicate.class), any(Predicate.class));
        verify(cb, times(2)).like(any(Expression.class), any(String.class));
    }

    @Test
    void hasColorShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteSpecifications.hasColor(null).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void hasColorShouldMatchNormalizedValue() {
        Path<String> color = mock(Path.class);
        when(root.get(Note_.color)).thenReturn(color);
        when(cb.equal(color, "#fff")).thenReturn(predicate);

        Predicate result = NoteSpecifications.hasColor(" #FfF ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(color, "#fff");
    }

    @Test
    void isPinnedShouldReturnConjunctionWhenNull() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteSpecifications.isPinned(null).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void isPinnedShouldMatchValue() {
        Path<Boolean> pinned = mock(Path.class);
        when(root.get(Note_.pinned)).thenReturn(pinned);
        when(cb.equal(pinned, Boolean.TRUE)).thenReturn(predicate);

        Predicate result = NoteSpecifications.isPinned(true).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(pinned, Boolean.TRUE);
    }

    @Test
    void hasTagsShouldReturnConjunctionWhenEmpty() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteSpecifications.hasTags(Set.of()).toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
    }

    @Test
    void hasTagsShouldFilterBlankTagsAndJoin() {
        @SuppressWarnings("unchecked")
        SetJoin<Note, Tag> join = mock(SetJoin.class);
        Path<String> tagNamePath = mock(Path.class);
        Predicate inPredicate = mock(Predicate.class);

        doReturn(join).when(root).join(Note_.tags, JoinType.LEFT);
        when(join.get(Tag_.name)).thenReturn(tagNamePath);
        when(tagNamePath.in(anyList())).thenReturn(inPredicate);

        Predicate result =
                NoteSpecifications.hasTags(Set.of("Java", "  ", "spring"))
                        .toPredicate(root, query, cb);

        assertThat(result).isSameAs(inPredicate);
        verify(query).distinct(true);
        verify(tagNamePath).in(anyList());
    }

    @Test
    void ownedByShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = NoteSpecifications.ownedBy(" ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).conjunction();
        verifyNoMoreInteractions(cb);
    }

    @Test
    void ownedByShouldMatchTrimmedOwner() {
        Path<String> owner = mock(Path.class);
        when(root.get(Note_.owner)).thenReturn(owner);
        when(cb.equal(owner, "Alice")).thenReturn(predicate);

        Predicate result = NoteSpecifications.ownedBy(" Alice ").toPredicate(root, query, cb);

        assertThat(result).isSameAs(predicate);
        verify(cb).equal(owner, "Alice");
    }

    @Test
    void prioritizePinnedShouldAddPinnedSortWhenUnsorted() {
        ensureJpaMetamodelInitialized();
        doReturn("createdDate").when(AuditableEntity_.createdDate).getName();
        Pageable pageable = PageRequest.of(1, 10, Sort.unsorted());

        Pageable result = NoteSpecifications.prioritizePinned(pageable);

        Sort.Order pinned = result.getSort().getOrderFor(Note_.pinned.getName());
        assertThat(pinned).isNotNull();
        assertThat(pinned.getDirection()).isEqualTo(Sort.Direction.DESC);
        Sort.Order created = result.getSort().getOrderFor(AuditableEntity_.createdDate.getName());
        assertThat(created).isNotNull();
        assertThat(created.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void prioritizePinnedShouldPrependPinnedWhenSorted() {
        ensureJpaMetamodelInitialized();
        Pageable pageable = PageRequest.of(0, 5, Sort.by("title").ascending());

        Pageable result = NoteSpecifications.prioritizePinned(pageable);

        Sort.Order pinned = result.getSort().getOrderFor(Note_.pinned.getName());
        assertThat(pinned).isNotNull();
        assertThat(pinned.getDirection()).isEqualTo(Sort.Direction.DESC);
        Sort.Order title = result.getSort().getOrderFor("title");
        assertThat(title).isNotNull();
    }

    private static void ensureJpaMetamodelInitialized() {
        Note_.pinned = namedBooleanAttribute("pinned");
        AuditableEntity_.createdDate = namedInstantAttribute();
    }

    @SuppressWarnings("unchecked")
    private static <T> SingularAttribute<T, Boolean> namedBooleanAttribute(String name) {
        SingularAttribute<T, Boolean> attribute = mock(SingularAttribute.class);
        doReturn(name).when(attribute).getName();
        return attribute;
    }

    @SuppressWarnings("unchecked")
    private static <T> SingularAttribute<T, Instant> namedInstantAttribute() {
        return mock(SingularAttribute.class);
    }
}
