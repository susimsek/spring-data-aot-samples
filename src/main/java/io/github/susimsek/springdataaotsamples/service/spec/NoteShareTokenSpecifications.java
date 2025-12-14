package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class NoteShareTokenSpecifications {

    public static Specification<NoteShareToken> ownedBy(String owner) {
        if (!StringUtils.hasText(owner)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> cb.equal(root.get("note").get("owner"), owner);
    }

    public static Specification<NoteShareToken> forNote(Long noteId) {
        if (noteId == null) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> cb.equal(root.get("note").get("id"), noteId);
    }

    public static Specification<NoteShareToken> search(String q) {
        if (!StringUtils.hasText(q)) {
            return (root, query, cb) -> cb.conjunction();
        }
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("tokenHash")), like),
                cb.like(cb.lower(root.get("note").get("title")), like)
        );
    }
}
