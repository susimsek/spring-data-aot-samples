package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.Tag_;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

@UtilityClass
public class TagSpecifications {

    public Specification<Tag> search(@Nullable String query) {
        if (!StringUtils.hasText(query)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        var like = "%" + query.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get(Tag_.name)), like);
    }

    public Specification<Tag> startsWith(@Nullable String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return (root, cq, cb) -> cb.disjunction();
        }
        var like = prefix.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get(Tag_.name)), like);
    }
}
