package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {

    List<Tag> findByNameIn(Collection<String> names);

    @Query("""
        select t.id
        from Tag t
        where not exists (
            select 1
            from Note n
            join n.tags tag
            where tag = t
        )
        """)
    List<Long> findOrphanIds();
}
