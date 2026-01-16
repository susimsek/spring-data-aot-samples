package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.domain.User_;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

@UtilityClass
public class UserSpecifications {

    public Specification<User> usernameContains(String query) {
        if (!StringUtils.hasText(query)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, cq, cb) -> cb.like(root.get(User_.username), like);
    }
}
