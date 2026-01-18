package io.github.susimsek.springdataaotsamples.repository.custom;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface NoteShareTokenRepositoryCustom {

    Page<Long> findIds(Specification<NoteShareToken> specification, Pageable pageable);
}
