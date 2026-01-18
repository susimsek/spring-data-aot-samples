package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.Note;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

public class NoteRepositoryCustomImpl implements NoteRepositoryCustom {

    @PersistenceContext private EntityManager em;

    @Override
    public Page<Note> findAllWithTags(Specification<Note> specification, Pageable pageable) {
        // Phase 1: ID page query
        List<Long> ids = fetchPageIds(specification, pageable);
        // Phase 2: total count query
        long total = countTotalNotes(specification);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // Phase 3: entity fetch query (with tags)
        List<Note> loadedNotes = fetchNotesWithTags(ids);

        Map<Long, Note> noteMap =
                loadedNotes.stream().collect(Collectors.toMap(Note::getId, n -> n, (a, b) -> a));
        var ordered = ids.stream().map(noteMap::get).toList();

        return new PageImpl<>(ordered, pageable, total);
    }

    private List<Long> fetchPageIds(Specification<Note> specification, Pageable pageable) {
        var cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
        Root<Note> root = idQuery.from(Note.class);
        idQuery.select(root.get("id"));

        applySpecification(specification, cb, idQuery, root);
        applySort(pageable, cb, idQuery, root);

        idQuery.distinct(false);

        var typed = em.createQuery(idQuery);
        typed.setFirstResult((int) pageable.getOffset());
        typed.setMaxResults(pageable.getPageSize());
        return typed.getResultList();
    }

    private long countTotalNotes(Specification<Note> specification) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Note> root = countQuery.from(Note.class);
        countQuery.select(cb.count(root));
        applySpecification(specification, cb, countQuery, root);
        return em.createQuery(countQuery).getSingleResult();
    }

    private List<Note> fetchNotesWithTags(Collection<Long> ids) {
        return em.createQuery(
                        """
                        select n
                        from Note n
                        left join fetch n.tags t
                        where n.id in :ids
                        """,
                        Note.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    private void applySpecification(
            Specification<Note> specification,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Note> root) {
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
    }

    private void applySort(
            Pageable pageable, CriteriaBuilder cb, CriteriaQuery<?> query, Root<Note> root) {
        if (pageable.getSort().isSorted()) {
            query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }
    }
}
