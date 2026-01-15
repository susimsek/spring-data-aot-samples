package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.Tag_;
import java.util.Locale;
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
        var like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, cq, cb) -> cb.like(root.get(Tag_.name), like);
    }

    public Specification<Tag> startsWith(@Nullable String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return (root, cq, cb) -> cb.disjunction();
        }
        var like = prefix.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, cq, cb) -> cb.like(root.get(Tag_.name), like);
    }
}
