package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.SoftDeletableEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeletableEntity, ID> {

    @Modifying
    @Transactional
    @Query("update #{#entityName} e set e.deleted = true where e.id in :ids and e.deleted = false")
    int softDeleteByIds(@Param("ids") Iterable<ID> ids);

    @Modifying
    @Transactional
    @Query("update #{#entityName} e set e.deleted = true where e.id = :id and e.deleted = false")
    int softDeleteById(@Param("id") ID id);

    @Modifying
    @Transactional
    @Query("update #{#entityName} e set e.deleted = false, e.deletedBy = null, e.deletedDate = null where e.id in :ids and e.deleted = true")
    int restoreByIds(@Param("ids") Iterable<ID> ids);

    @Modifying
    @Transactional
    @Query("update #{#entityName} e set e.deleted = false, e.deletedBy = null, e.deletedDate = null where e.id = :id and e.deleted = true")
    int restoreById(@Param("id") ID id);

    @Modifying
    @Transactional
    @Query("delete from #{#entityName} e where e.deleted = true")
    void purgeDeleted();
}
