package io.github.susimsek.springdataaotsamples.service.spec;

import io.github.susimsek.springdataaotsamples.domain.AuditableEntity_;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Note_;
import io.github.susimsek.springdataaotsamples.domain.SoftDeletableEntity_;
import io.github.susimsek.springdataaotsamples.domain.Tag_;
import jakarta.persistence.criteria.JoinType;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

@UtilityClass
public class NoteSpecifications {

  public Specification<Note> isNotDeleted() {
    return (root, cq, cb) -> cb.isFalse(root.get(SoftDeletableEntity_.deleted));
  }

  public Specification<Note> isDeleted() {
    return (root, cq, cb) -> cb.isTrue(root.get(SoftDeletableEntity_.deleted));
  }

  public Specification<Note> search(@Nullable String query) {
    if (!StringUtils.hasText(query)) {
      return (root, cq, cb) -> cb.conjunction();
    }
    return (root, cq, cb) -> {
      var like = "%" + query.toLowerCase() + "%";
      var title = cb.like(cb.lower(root.get(Note_.title)), like);
      var content = cb.like(cb.lower(root.get(Note_.content)), like);
      return cb.or(title, content);
    };
  }

  public Specification<Note> hasColor(@Nullable String color) {
    if (!StringUtils.hasText(color)) {
      return (root, cq, cb) -> cb.conjunction();
    }
    return (root, cq, cb) -> cb.equal(cb.lower(root.get(Note_.color)), color);
  }

  public Specification<Note> isPinned(@Nullable Boolean pinned) {
    if (pinned == null) {
      return (root, cq, cb) -> cb.conjunction();
    }
    return (root, cq, cb) -> cb.equal(root.get(Note_.pinned), pinned);
  }

  public Specification<Note> hasTags(@Nullable Set<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return (root, cq, cb) -> cb.conjunction();
    }
    var normalized =
        tags.stream().filter(StringUtils::hasText).map(tag -> tag.trim().toLowerCase()).toList();
    if (normalized.isEmpty()) {
      return (root, cq, cb) -> cb.conjunction();
    }
    return (root, cq, cb) -> {
      // Ensure we don't duplicate rows due to join
      cq.distinct(true);
      var join = root.join(Note_.tags, JoinType.LEFT);
      return cb.lower(join.get(Tag_.name)).in(normalized);
    };
  }

  public Specification<Note> ownedBy(@Nullable String owner) {
    if (!StringUtils.hasText(owner)) {
      return (root, cq, cb) -> cb.conjunction();
    }
    return (root, cq, cb) -> cb.equal(cb.lower(root.get(Note_.owner)), owner.trim().toLowerCase());
  }

  public Pageable prioritizePinned(Pageable pageable) {
    Sort baseSort =
        pageable.getSort().isUnsorted()
            ? Sort.by(
                Sort.Order.desc(Note_.pinned.getName()),
                Sort.Order.desc(AuditableEntity_.createdDate.getName()))
            : Sort.by(Sort.Order.desc(Note_.pinned.getName())).and(pageable.getSort());
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), baseSort);
  }
}
