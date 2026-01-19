package io.github.susimsek.springdataaotsamples.repository.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.support.PageableUtils;

public abstract class JpaSpecificationPagingSupport<T, I> {

    protected final EntityManager entityManager;
    protected final JpaEntityInformation<T, ?> entityInformation;

    protected JpaSpecificationPagingSupport(
            JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    protected final Page<T> findAll(
            Specification<T> specification,
            Pageable pageable,
            Function<Collection<I>, List<T>> fetchEntities) {
        List<I> ids = fetchPageIds(specification, pageable);
        long total = countTotal(specification);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<T> loadedEntities = fetchEntities.apply(ids);
        Map<I, T> byId = HashMap.newHashMap(loadedEntities.size());
        for (T entity : loadedEntities) {
            I id = getId(entity);
            byId.putIfAbsent(id, entity);
        }

        List<T> ordered = ids.stream().map(byId::get).toList();
        return new PageImpl<>(ordered, pageable, total);
    }

    private List<I> fetchPageIds(Specification<T> specification, Pageable pageable) {
        @SuppressWarnings("unchecked")
        Class<I> idType = (Class<I>) entityInformation.getIdType();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<I> idQuery = cb.createQuery(idType);
        Root<T> root = idQuery.from(entityInformation.getJavaType());
        String path = getIdAttributeName();
        idQuery.select(root.get(path).as(idType));

        applySpecification(specification, cb, idQuery, root);
        applySort(pageable, cb, idQuery, root);

        idQuery.distinct(false);

        var typed = entityManager.createQuery(idQuery);
        if (pageable.isPaged()) {
            typed.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
            typed.setMaxResults(pageable.getPageSize());
        }
        return typed.getResultList();
    }

    private long countTotal(Specification<T> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> root = countQuery.from(entityInformation.getJavaType());
        countQuery.select(cb.count(root));
        applySpecification(specification, cb, countQuery, root);
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private String getIdAttributeName() {
        var idAttribute = entityInformation.getIdAttribute();
        if (idAttribute == null) {
            throw new IllegalStateException("Only single id attributes are supported.");
        }
        return idAttribute.getName();
    }

    @SuppressWarnings("unchecked")
    private I getId(T entity) {
        return (I) entityInformation.getId(entity);
    }

    private void applySpecification(
            Specification<T> specification,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<T> root) {
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
    }

    private void applySort(
            Pageable pageable, CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root) {
        if (pageable.getSort().isSorted()) {
            query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }
    }
}
