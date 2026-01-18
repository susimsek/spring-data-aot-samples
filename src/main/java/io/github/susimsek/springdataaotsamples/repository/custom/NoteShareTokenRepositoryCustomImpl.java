package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

public class NoteShareTokenRepositoryCustomImpl implements NoteShareTokenRepositoryCustom {

    @PersistenceContext private EntityManager em;

    @Override
    public Page<Long> findIds(Specification<NoteShareToken> specification, Pageable pageable) {
        var cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
        Root<NoteShareToken> root = idQuery.from(NoteShareToken.class);
        idQuery.select(root.get("id"));

        Predicate predicate = applySpecification(specification, cb, idQuery, root);
        idQuery.where(predicate);

        if (pageable.getSort().isSorted()) {
            idQuery.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }

        idQuery.distinct(false);

        var typed = em.createQuery(idQuery);
        typed.setFirstResult((int) pageable.getOffset());
        typed.setMaxResults(pageable.getPageSize());
        List<Long> ids = typed.getResultList();

        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        long total = count(specification);
        return new PageImpl<>(ids, pageable, total);
    }

    private long count(Specification<NoteShareToken> specification) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<NoteShareToken> root = countQuery.from(NoteShareToken.class);
        countQuery.select(cb.countDistinct(root));
        Predicate predicate = applySpecification(specification, cb, countQuery, root);
        countQuery.where(predicate);
        return em.createQuery(countQuery).getSingleResult();
    }

    private Predicate applySpecification(
            Specification<NoteShareToken> specification,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<NoteShareToken> root) {
        return specification.toPredicate(root, query, cb);
    }
}
