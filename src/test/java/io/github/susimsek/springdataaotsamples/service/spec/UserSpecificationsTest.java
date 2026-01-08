package io.github.susimsek.springdataaotsamples.service.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.domain.User_;
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
class UserSpecificationsTest {

    @Mock private Root<User> root;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder cb;

    @Mock private Predicate conjunction;

    @Test
    void usernameContainsShouldReturnConjunctionWhenBlank() {
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<User> specification = UserSpecifications.usernameContains("  ");
        Predicate predicate = specification.toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(conjunction);
        verify(cb).conjunction();
        verifyNoMoreInteractions(cb);
    }

    @Test
    void usernameContainsShouldCreateLikePredicate() {
        Path<String> usernamePath = mock(Path.class);
        Expression<String> lowerExpression = mock(Expression.class);
        Predicate likePredicate = mock(Predicate.class);

        when(root.get(User_.username)).thenReturn(usernamePath);
        when(cb.lower(usernamePath)).thenReturn(lowerExpression);
        when(cb.like(lowerExpression, "%alice%")).thenReturn(likePredicate);

        Specification<User> specification = UserSpecifications.usernameContains(" Alice ");
        Predicate predicate = specification.toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(likePredicate);
        verify(cb).like(lowerExpression, "%alice%");
    }
}
