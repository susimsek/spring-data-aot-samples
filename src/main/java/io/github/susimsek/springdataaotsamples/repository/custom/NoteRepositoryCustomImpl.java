package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.Note;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;

public class NoteRepositoryCustomImpl extends JpaSpecificationPagingSupport<Note, Long>
        implements NoteRepositoryCustom {

    private static final String FETCH_WITH_TAGS_QUERY =
            """
            select n
            from Note n
            left join fetch n.tags t
            where n.id in :ids
            """;

    public NoteRepositoryCustomImpl(EntityManager entityManager) {
        super(
                JpaEntityInformationSupport.getEntityInformation(Note.class, entityManager),
                entityManager);
    }

    @Override
    public Page<Note> findAllWithTags(Specification<Note> specification, Pageable pageable) {
        return findAll(
                specification,
                pageable,
                ids ->
                        entityManager
                                .createQuery(FETCH_WITH_TAGS_QUERY, Note.class)
                                .setParameter("ids", ids)
                                .getResultList());
    }
}
