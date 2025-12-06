package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.Note;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.util.List;

public class NoteRepositoryCustomImpl implements NoteRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Long> findIds(@Nullable Specification<Note> specification,
                              Pageable pageable) {
        var cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
        Root<Note> root = idQuery.from(Note.class);
        idQuery.select(root.get("id"));

        Predicate predicate = applySpecification(specification, cb, idQuery, root);
        if (predicate != null) {
            idQuery.where(predicate);
        }
        if (pageable.getSort().isSorted()) {
            idQuery.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }
        // Avoid DISTINCT + ORDER BY select columns issue (e.g. H2) by disabling distinct and de-duplicating in memory
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

    private long count(Specification<Note> specification) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Note> root = countQuery.from(Note.class);
        countQuery.select(cb.countDistinct(root));
        Predicate predicate = applySpecification(specification, cb, countQuery, root);
        if (predicate != null) {
            countQuery.where(predicate);
        }
        return em.createQuery(countQuery).getSingleResult();
    }

    private Predicate applySpecification(
            Specification<Note> specification,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Note> root
    ) {
        if (specification == null) {
            return null;
        }
        return specification.toPredicate(root, query, cb);
    }
}
