package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;

public class NoteShareTokenRepositoryCustomImpl
        extends JpaSpecificationPagingSupport<NoteShareToken, Long>
        implements NoteShareTokenRepositoryCustom {

    private static final String FETCH_WITH_NOTE_QUERY =
            """
            select t
            from NoteShareToken t
            join fetch t.note n
            where t.id in :ids
            """;

    public NoteShareTokenRepositoryCustomImpl(EntityManager entityManager) {
        super(
                JpaEntityInformationSupport.getEntityInformation(
                        NoteShareToken.class, entityManager),
                entityManager);
    }

    @Override
    public Page<NoteShareToken> findAllWithNote(
            Specification<NoteShareToken> specification, Pageable pageable) {
        return findAll(
                specification,
                pageable,
                ids ->
                        entityManager
                                .createQuery(FETCH_WITH_NOTE_QUERY, NoteShareToken.class)
                                .setParameter("ids", ids)
                                .getResultList());
    }
}
