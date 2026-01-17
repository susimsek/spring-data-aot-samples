package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.AuditableEntity_;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken_;
import io.github.susimsek.springdataaotsamples.domain.Note_;
import java.time.Instant;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

@UtilityClass
public class NoteShareTokenSpecifications {

    public static Specification<NoteShareToken> ownedBy(String owner) {
        if (!StringUtils.hasText(owner)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> cb.equal(root.get(NoteShareToken_.note).get(Note_.owner), owner);
    }

    public static Specification<NoteShareToken> forNote(@Nullable Long noteId) {
        if (noteId == null) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> cb.equal(root.get(NoteShareToken_.note).get(Note_.id), noteId);
    }

    public static Specification<NoteShareToken> search(String q) {
        if (!StringUtils.hasText(q)) {
            return (root, query, cb) -> cb.conjunction();
        }
        String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(root.get(NoteShareToken_.tokenHash), like),
                        cb.like(cb.lower(root.get(NoteShareToken_.note).get(Note_.title)), like));
    }

    public static Specification<NoteShareToken> status(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return (root, cq, cb) -> cb.conjunction();
        }
        String val = status.toLowerCase();
        return (root, cq, cb) ->
                switch (val) {
                    case "revoked" -> cb.isTrue(root.get(NoteShareToken_.revoked));
                    case "expired" ->
                            cb.and(
                                    cb.isNotNull(root.get(NoteShareToken_.expiresAt)),
                                    cb.lessThan(
                                            root.get(NoteShareToken_.expiresAt),
                                            Instant.now()));
                    case "active" ->
                            cb.and(
                                    cb.isFalse(root.get(NoteShareToken_.revoked)),
                                    cb.or(
                                            cb.isNull(root.get(NoteShareToken_.expiresAt)),
                                            cb.greaterThan(
                                                    root.get(NoteShareToken_.expiresAt),
                                                    Instant.now())));
                    default -> cb.conjunction();
                };
    }

    public static Specification<NoteShareToken> createdBetween(
            @Nullable Instant from, @Nullable Instant to) {
        if (from == null && to == null) {
            return (root, cq, cb) -> cb.conjunction();
        }
        return (root, cq, cb) -> {
            var predicate = cb.conjunction();
            if (from != null) {
                predicate =
                        cb.and(
                                predicate,
                                cb.greaterThanOrEqualTo(
                                        root.get(AuditableEntity_.createdDate), from));
            }
            if (to != null) {
                predicate =
                        cb.and(
                                predicate,
                                cb.lessThanOrEqualTo(root.get(AuditableEntity_.createdDate), to));
            }
            return predicate;
        };
    }
}
