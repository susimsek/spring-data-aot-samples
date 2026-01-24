package io.github.susimsek.springdataaotsamples.repository.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.support.PageableExecutionUtils;

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
        if (ids.isEmpty()) {
            return PageableExecutionUtils.getPage(
                    Collections.emptyList(), pageable, () -> countTotal(specification));
        }

        List<T> loadedEntities = fetchEntities.apply(ids);
        Map<I, T> byId = HashMap.newHashMap(loadedEntities.size());
        for (T entity : loadedEntities) {
            I id = getId(entity);
            byId.putIfAbsent(id, entity);
        }

        List<T> ordered = ids.stream().map(byId::get).toList();
        return PageableExecutionUtils.getPage(ordered, pageable, () -> countTotal(specification));
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
        List<Order> orders = buildOrders(pageable, cb, root);
        if (!orders.isEmpty()) {
            idQuery.orderBy(orders);
        }
        if (idQuery.isDistinct()) {
            idQuery.distinct(false);
            if (orders.isEmpty()) {
                idQuery.groupBy(root.get(path));
            } else {
                idQuery.groupBy(buildGroupBy(root.get(path), orders));
            }
        }

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

    private List<Order> buildOrders(Pageable pageable, CriteriaBuilder cb, Root<T> root) {
        if (pageable.getSort().isSorted()) {
            return QueryUtils.toOrders(pageable.getSort(), root, cb);
        }
        return List.of();
    }

    private List<Expression<?>> buildGroupBy(Expression<?> id, List<Order> orders) {
        Set<Expression<?>> groupBy = new LinkedHashSet<>();
        groupBy.add(id);
        for (Order order : orders) {
            groupBy.add(order.getExpression());
        }
        return new ArrayList<>(groupBy);
    }
}
