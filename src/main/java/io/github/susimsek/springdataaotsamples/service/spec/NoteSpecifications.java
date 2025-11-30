package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.Note;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

@UtilityClass
public class NoteSpecifications {

    public Specification<Note> isNotDeleted() {
        return (root, cq, cb) -> cb.isFalse(root.get("deleted"));
    }

    public Specification<Note> isDeleted() {
        return (root, cq, cb) -> cb.isTrue(root.get("deleted"));
    }

    public Specification<Note> search(String query) {
        if (!StringUtils.hasText(query)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> {
            var like = "%" + query.toLowerCase() + "%";
            var title = cb.like(cb.lower(root.get("title")), like);
            var content = cb.like(cb.lower(root.get("content")), like);
            return cb.or(title, content);
        };
    }

    public Pageable prioritizePinned(Pageable pageable) {
        Sort baseSort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdDate"))
                : Sort.by(Sort.Order.desc("pinned")).and(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), baseSort);
    }
}
