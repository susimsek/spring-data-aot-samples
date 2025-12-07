package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.User;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

@UtilityClass
public class UserSpecifications {

    public Specification<User> usernameContains(String query) {
        if (!StringUtils.hasText(query)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        String like = "%" + query.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("username")), like);
    }
}
