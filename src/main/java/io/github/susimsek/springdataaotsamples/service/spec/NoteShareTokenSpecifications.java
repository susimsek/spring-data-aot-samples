package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;

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

    public static Specification<NoteShareToken> status(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        String val = status.toLowerCase();
        return (root, cq, cb) -> switch (val) {
            case "revoked" -> cb.isTrue(root.get("revoked"));
            case "expired" -> cb.and(
                    cb.isNotNull(root.get("expiresAt")),
                    cb.lessThan(root.get("expiresAt"), java.time.Instant.now())
            );
            case "active" -> cb.and(
                    cb.isFalse(root.get("revoked")),
                    cb.or(
                            cb.isNull(root.get("expiresAt")),
                            cb.greaterThan(root.get("expiresAt"), java.time.Instant.now())
                    )
            );
            default -> cb.conjunction();
        };
    }

    public static Specification<NoteShareToken> createdBetween(Instant from, Instant to) {
        if (from == null && to == null) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> {
            var predicate = cb.conjunction();
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdDate"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdDate"), to));
            }
            return predicate;
        };
    }

}
