package io.github.susimsek.springdataaotsamples.service.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.Tag_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TagSpecificationsTest {

    @Mock private Root<Tag> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder cb;

    @Mock private Predicate conjunction;

    @Mock private Predicate disjunction;

    @Test
    void searchShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Tag> specification = TagSpecifications.search(" ");
        Predicate predicate = specification.toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(conjunction);
        verify(cb).conjunction();
        verifyNoMoreInteractions(cb);
    }

    @Test
    void searchShouldCreateLikePredicate() {
        Path<String> namePath = mock(Path.class);
        Expression<String> lower = mock(Expression.class);
        Predicate like = mock(Predicate.class);

        when(root.get(Tag_.name)).thenReturn(namePath);
        when(cb.lower(namePath)).thenReturn(lower);
        when(cb.like(lower, "%java%")).thenReturn(like);

        Specification<Tag> specification = TagSpecifications.search(" Java ");
        Predicate predicate = specification.toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(like);
        verify(cb).like(lower, "%java%");
    }

    @Test
    void startsWithShouldReturnDisjunctionWhenBlank() {
        when(cb.disjunction()).thenReturn(disjunction);

        Specification<Tag> specification = TagSpecifications.startsWith("");
        Predicate predicate = specification.toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(disjunction);
        verify(cb).disjunction();
        verifyNoMoreInteractions(cb);
    }

    @Test
    void startsWithShouldCreatePrefixPredicate() {
        Path<String> namePath = mock(Path.class);
        Expression<String> lower = mock(Expression.class);
        Predicate like = mock(Predicate.class);

        when(root.get(Tag_.name)).thenReturn(namePath);
        when(cb.lower(namePath)).thenReturn(lower);
        when(cb.like(lower, "spr%")).thenReturn(like);

        Specification<Tag> specification = TagSpecifications.startsWith("  Spr ");
        Predicate predicate = specification.toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(like);
        verify(cb).like(lower, "spr%");
    }
}
